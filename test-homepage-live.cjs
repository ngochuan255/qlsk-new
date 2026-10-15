/* Read-only checks of the packaged application and its real public event API. */
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { chromium } = require(process.env.PLAYWRIGHT_MODULE_PATH || 'playwright-core');
const base = process.env.EVENTHUB_BASE_URL || 'http://127.0.0.1:8080';
const output = path.join(__dirname, 'target/homepage-checks');

(async () => {
    fs.mkdirSync(output, {recursive:true});
    const browser = await chromium.launch({headless:true, executablePath:process.env.CHROME_PATH || 'C:/Program Files/Google/Chrome/Application/chrome.exe'});
    const report = {base, checks:[], metrics:[], errors:[]};
    try {
        const context = await browser.newContext();
        const response = await context.request.get(base + '/api/events/public');
        assert.equal(response.status(),200);
        const events = await response.json();
        for (const asset of ['/css/home.css','/js/home.js','/js/home-motion.js','/js/vendor/gsap/gsap.min.js','/js/vendor/gsap/ScrollTrigger.min.js','/css/fonts/plus-jakarta-latin-variable.woff2','/css/fonts/plus-jakarta-vietnamese-variable.woff2']) {
            assert.equal((await context.request.get(base+asset)).status(),200,`public asset ${asset}`);
        }
        for (const width of [375,768,1024,1440]) {
            const page = await browser.newPage({hasTouch:width===375,isMobile:width===375});
            page.setDefaultTimeout(10000);
            await page.setViewportSize({width,height:width===375?812:1000});
            page.on('pageerror',error=>report.errors.push(error.message));
            await page.addInitScript(()=>{
                window.homePerf={lcp:0,cls:0};
                new PerformanceObserver(list=>{for(const e of list.getEntries()) window.homePerf.lcp=e.startTime;}).observe({type:'largest-contentful-paint',buffered:true});
                new PerformanceObserver(list=>{for(const e of list.getEntries()) if(!e.hadRecentInput) window.homePerf.cls+=e.value;}).observe({type:'layout-shift',buffered:true});
            });
            const documentResponse = await page.goto(base,{waitUntil:'networkidle'});
            assert.match(documentResponse.headers()['content-type'],/charset=UTF-8/i);
            await page.waitForFunction(()=>document.querySelector('#events-grid')?.getAttribute('aria-busy')==='false');
            assert.equal(await page.locator('#events-grid .event-card').count(),events.length);
            assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),`overflow at ${width}`);
            assert.equal(await page.getByRole('heading',{name:'Khám phá sự kiện. Kết nối trải nghiệm.'}).count(),1);
            assert.ok(await page.evaluate(()=>!!window.gsap && !!window.ScrollTrigger));
            if(events[0]) assert.ok((await page.locator('#events-grid').textContent()).includes(events[0].title));
            const perf = await page.evaluate(()=>window.homePerf);
            report.metrics.push({width,...perf,conditions:'Headless Chrome; local server; no network or CPU throttling'});
            if(width===1440) await page.screenshot({path:path.join(output,'live-desktop-hero.png')});
            if(width===375) {
                await page.getByRole('button',{name:'Mở menu điều hướng'}).click();
                await page.getByRole('button',{name:'Đóng menu điều hướng'}).click();
                const client=await page.context().newCDPSession(page);
                await client.send('Input.dispatchTouchEvent',{type:'touchStart',touchPoints:[{x:180,y:620}]});
                for(let y=600;y>=200;y-=40) {
                    await client.send('Input.dispatchTouchEvent',{type:'touchMove',touchPoints:[{x:180,y}]});
                    await page.waitForTimeout(20);
                }
                await client.send('Input.dispatchTouchEvent',{type:'touchEnd',touchPoints:[]});
                await page.waitForTimeout(250);
                assert.ok(await page.evaluate(()=>scrollY)>0,'native touch scrolling');
                await client.detach();
            }
            for(const selector of ['#events','#how-it-works','.journey-cta']) {
                await page.locator(selector).scrollIntoViewIfNeeded(); await page.waitForTimeout(650);
            }
            assert.ok(await page.evaluate(()=>document.fonts.check('500 40px "EventHub Scroll"','Đến một cuộc gặp.')),'local variable font including Vietnamese loaded');
            if(width===1440) {
                const bounds=await page.evaluate(()=>{
                    const trigger=ScrollTrigger.getAll().find(item=>item.trigger===document.getElementById('journey-title'));
                    return trigger ? {start:trigger.start,end:trigger.end} : null;
                });
                assert.ok(bounds,'live scroll text trigger');
                await page.evaluate(y=>scrollTo({top:y,behavior:'instant'}),bounds.start+(bounds.end-bounds.start)*.45);
                await page.waitForTimeout(900);
                const midWeight=await page.locator('#journey-title [data-scroll-line]').first().evaluate(el=>Number(getComputedStyle(el).fontWeight));
                assert.ok(midWeight>440 && midWeight<730,`live continuous weight: ${midWeight}`);
                await page.evaluate(y=>scrollTo({top:y,behavior:'instant'}),bounds.end);
                await page.waitForTimeout(900);
                await page.screenshot({path:path.join(output,'live-scroll-text.png')});
            }
            if(width===1440 || width===375) {
                await page.evaluate(()=>scrollTo({top:0,behavior:'instant'}));
                await page.waitForTimeout(450);
                await page.screenshot({path:path.join(output,`live-${width===375?'mobile':'desktop'}.png`),fullPage:true});
            }
            report.checks.push({width,eventCount:events.length,passed:true});
            await page.close();
        }
        assert.deepEqual(report.errors,[]);
        fs.writeFileSync(path.join(output,'live-report.json'),JSON.stringify(report,null,2));
        console.log(JSON.stringify(report,null,2));
    } finally {await browser.close();}
})().catch(error=>{console.error(error);process.exitCode=1;});
