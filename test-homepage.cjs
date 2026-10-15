/* Browser acceptance checks. Set PLAYWRIGHT_MODULE_PATH to an installed playwright-core module. */
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const http = require('node:http');
const { chromium } = require(process.env.PLAYWRIGHT_MODULE_PATH || 'playwright-core');

const staticRoot = path.join(__dirname, 'src/main/resources/static');
const resultsRoot = path.join(__dirname, 'target/homepage-checks');
const posterPath = process.env.EVENTHUB_POSTER_PATH;
const event = {
  id: 1, title: 'Hội thảo Công nghệ & AI thế hệ mới', categoryId: 1,
  categoryName: 'Công nghệ & AI', organizerName: 'EventHub',
  startTime: '2026-11-20T09:00:00', endTime: '2026-11-20T17:00:00',
  location: 'Hội trường A, Hà Nội', remainingSeats: 32, maxAttendees: 150,
  averageRating: 4.8, feedbackCount: 12, status: 'OPEN_FOR_REGISTRATION',
  statusText: 'Đang mở đăng ký', bannerUrl: '/uploads/homepage-test-poster.png'
};
const categories = [{ id: 1, name: 'Công nghệ & AI' }, { id: 2, name: 'Văn hóa & Nghệ thuật' }];
const delay = ms => new Promise(resolve => setTimeout(resolve, ms));
let checks = 0;
const check = async (name, fn) => {
  if (process.env.HOMEPAGE_CHECK_FILTER && !name.includes(process.env.HOMEPAGE_CHECK_FILTER)) return;
  await fn(); checks++; console.log(`PASS ${name}`);
};

async function main() {
  fs.mkdirSync(resultsRoot, { recursive: true });
  const server = http.createServer((req, res) => {
    const url = new URL(req.url, 'http://localhost');
    let filename;
    try {
      filename = path.resolve(staticRoot, '.' + (decodeURIComponent(url.pathname) === '/' ? '/index.html' : decodeURIComponent(url.pathname)));
    } catch { res.writeHead(400).end(); return; }
    if (url.pathname === '/uploads/homepage-test-poster.png' && posterPath) filename = posterPath;
    else if (!filename.startsWith(staticRoot + path.sep)) { res.writeHead(403).end(); return; }
    if (!fs.existsSync(filename) || fs.statSync(filename).isDirectory()) { res.writeHead(404).end(); return; }
    const type = { '.html':'text/html; charset=utf-8', '.js':'text/javascript; charset=utf-8', '.css':'text/css; charset=utf-8', '.svg':'image/svg+xml', '.png':'image/png', '.woff2':'font/woff2' }[path.extname(filename)] || 'application/octet-stream';
    res.writeHead(200, {'Content-Type': type}); fs.createReadStream(filename).pipe(res);
  });
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  const base = `http://127.0.0.1:${server.address().port}`;
  const browser = await chromium.launch({ headless: true, executablePath: process.env.CHROME_PATH || 'C:/Program Files/Google/Chrome/Application/chrome.exe' });
  const errors = [];
  const newPage = async (events = [event], options = {}) => {
    const {user, ...contextOptions} = options;
    const page = await browser.newPage({ viewport: {width:1440, height:1000}, ...contextOptions });
    page.setDefaultTimeout(8000);
    if (user) await page.addInitScript(user => {
      localStorage.setItem('user', JSON.stringify(user));
      localStorage.setItem('token', 'browser-fixture');
    }, user);
    page.on('pageerror', error => errors.push(error.message));
    await page.route('**/api/categories', route => route.fulfill({json: categories}));
    await page.route('**/api/events/public**', route => route.fulfill({json: events}));
    await page.goto(base, { waitUntil: 'networkidle' });
    return page;
  };
  try {
    await check('hero, search and live event card are available', async () => {
      const page = await newPage();
      await page.getByRole('heading', {name: 'Khám phá sự kiện. Kết nối trải nghiệm.'}).waitFor();
      assert.equal(await page.locator('#events-grid .event-card').count(), 1);
      assert.equal(await page.locator('#featured-track .featured-card').count(), 1);
      assert.equal(await page.locator('#featured-controls').isVisible(), false);
      assert.equal(await page.getByLabel('Từ khóa').count(), 1);
      for (const id of ['#featured-section','#events','#how-it-works','.journey-cta']) {
        await page.locator(id).scrollIntoViewIfNeeded(); await delay(650);
      }
      await page.evaluate(()=>scrollTo(0,0)); await delay(200);
      await page.screenshot({path:path.join(resultsRoot, 'desktop.png'), fullPage:true});
      await page.screenshot({path:path.join(resultsRoot, 'desktop-hero.png')});
      await page.close();
    });
    await check('responsive page never overflows at 375, 768, 1024 and 1440', async () => {
      const page = await newPage();
      for (const width of [375,768,1024,1440]) {
        await page.setViewportSize({width,height:900});
        await delay(180);
        assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1), `overflow at ${width}`);
        if(width === 375) {
          for (const id of ['#featured-section','#events','#how-it-works','.journey-cta']) {
            await page.locator(id).scrollIntoViewIfNeeded(); await delay(600);
          }
          await page.evaluate(()=>scrollTo(0,0));
          await page.screenshot({path:path.join(resultsRoot,'mobile.png'),fullPage:true});
        }
      }
      await page.close();
    });
    await check('empty catalogue and single or paired featured events have no carousel controls', async () => {
      for(const count of [0,1,2]) {
        const page = await newPage(Array.from({length:count},(_,i)=>({...event,id:i+1})));
        assert.equal(await page.locator('#featured-controls').isVisible(),false);
        assert.equal(await page.locator('#featured-section').isVisible(),count>0);
        if(!count) await page.getByText('Chưa có sự kiện phù hợp', {exact:true}).waitFor();
        await page.close();
      }
    });
    await check('featured carousel ranks statuses, has buttons and keyboard access', async () => {
      const events = ['COMPLETED','FULL','ONGOING','OPEN_FOR_REGISTRATION'].map((status,i)=>({...event,id:i+1,status,title:`Sự kiện ${status}`}));
      const page = await newPage(events);
      assert.equal(await page.locator('#featured-track .featured-card').count(),3);
      assert.match(await page.locator('#featured-track .featured-card').first().textContent(),/OPEN_FOR_REGISTRATION/);
      await page.locator('#featured-section').scrollIntoViewIfNeeded();
      await page.getByRole('button',{name:'Sự kiện tiếp theo'}).click();
      await delay(600);
      assert.ok(await page.locator('#featured-track').evaluate(el=>el.scrollLeft)>0);
      await page.locator('#featured-track').focus();
      await page.keyboard.press('Home');
      await delay(600);
      assert.ok(await page.locator('#featured-track').evaluate(el=>el.scrollLeft)<2);
      await page.close();
    });
    await check('filters update results, preserve input focus, and ignore stale requests', async () => {
      const page = await newPage();
      await page.unroute('**/api/events/public**');
      await page.route('**/api/events/public**', async route => {
        const word = new URL(route.request().url()).searchParams.get('keyword');
        await delay(word==='cũ'?400:30);
        await route.fulfill({json:[{...event,title:word||event.title}]});
      });
      await page.getByLabel('Từ khóa').fill('cũ');
      await page.getByLabel('Từ khóa').press('Enter');
      await page.getByLabel('Từ khóa').fill('mới');
      await page.getByLabel('Từ khóa').press('Enter');
      await page.waitForFunction(()=>document.querySelector('#events-grid h3')?.textContent==='mới');
      await delay(500);
      assert.equal(await page.locator('#events-grid h3').textContent(),'mới');
      assert.equal(await page.getByLabel('Từ khóa').evaluate(el=>document.activeElement===el),true);
      assert.match(await page.locator('#featured-track').textContent(),/Hội thảo/);
      await page.getByRole('button',{name:'Xóa bộ lọc',exact:true}).click();
      assert.equal(await page.getByLabel('Từ khóa').inputValue(),'');
      await page.close();
    });
    await check('mobile menu traps focus, closes with Escape and restores opener', async () => {
      const page = await newPage([event],{viewport:{width:375,height:812}});
      const toggle = page.getByRole('button',{name:'Mở menu điều hướng'});
      await toggle.click();
      assert.equal(await page.locator('#home-menu').getAttribute('aria-modal'),'true');
      await page.locator('#menu-close').focus();
      await page.keyboard.press('Shift+Tab');
      assert.equal(await page.locator('#home-menu a').last().evaluate(el=>document.activeElement===el),true);
      await page.keyboard.press('Escape');
      assert.equal(await toggle.getAttribute('aria-expanded'),'false');
      assert.equal(await toggle.evaluate(el=>document.activeElement===el),true);
      await page.close();
    });
    await check('account menus preserve each role, support long names and render names safely', async () => {
      for (const [role,link] of [['ROLE_ATTENDEE','/my-tickets.html'],['ROLE_ORGANIZER','/organizer/index.html'],['ADMIN','/admin/index.html']]) {
        const name = 'Nguyễn Minh Anh <img src=x onerror="window.badName=true">';
        const page = await newPage([event],{user:{role,fullName:name}});
        assert.equal(await page.locator('.home-auth img').count(),0);
        assert.equal(await page.locator(`.home-auth a[href="${link}"]`).count(),1);
        for (const width of [1024,1440]) {
          await page.setViewportSize({width,height:900});
          assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),`account overflow ${role} ${width}`);
        }
        await page.close();
      }
    });
    await check('failed API can retry, categories fail independently and broken posters fall back', async () => {
      const page = await browser.newPage();
      let fail = true;
      await page.route('**/api/categories',route=>route.fulfill({status:503,json:{message:'offline'}}));
      await page.route('**/api/events/public**',route=>fail ? route.fulfill({status:503,json:{message:'offline'}}) : route.fulfill({json:[{...event,bannerUrl:'/missing-poster.png'}]}));
      await page.goto(base,{waitUntil:'networkidle'});
      await page.getByRole('heading',{name:'Chưa thể tải sự kiện'}).waitFor();
      assert.equal(await page.locator('#category-notice').isVisible(),true);
      fail = false;
      await page.locator('#events-grid').getByRole('button',{name:'Thử lại'}).click();
      await page.locator('#events-grid .event-card .image-fallback').waitFor();
      assert.equal(await page.locator('#hero-media .image-fallback').count(),1);
      await page.close();
    });
    await check('combined filters send encoded values and event text cannot inject markup', async () => {
      const page = await newPage();
      const maliciousTitle = '<img src=x onerror=alert(1)> Hội thảo & Nghệ thuật';
      let query;
      await page.unroute('**/api/events/public**');
      await page.route('**/api/events/public**',route=>{
        query=new URL(route.request().url()).searchParams;
        return route.fulfill({json:[{...event,title:maliciousTitle,bannerUrl:'javascript:alert(1)'}]});
      });
      await page.getByLabel('Từ khóa').fill('Hà Nội & AI');
      await page.getByLabel('Danh mục',{exact:true}).selectOption('1');
      await page.getByLabel('Trạng thái',{exact:true}).selectOption('FULL');
      await page.getByRole('button',{name:'Tìm kiếm',exact:true}).click();
      await page.waitForFunction(()=>document.querySelector('#events-grid h3')?.textContent.includes('onerror'));
      assert.equal(query.get('keyword'),'Hà Nội & AI'); assert.equal(query.get('categoryId'),'1'); assert.equal(query.get('status'),'FULL');
      assert.equal(await page.locator('#events-grid h3').textContent(),maliciousTitle);
      assert.equal(await page.locator('#events-grid img').count(),0);
      await page.close();
    });
    await check('pointer drag moves the gallery without opening event links', async () => {
      const page=await newPage(Array.from({length:6},(_,i)=>({...event,id:i+1})));
      await page.locator('#featured-section').scrollIntoViewIfNeeded();
      const box=await page.locator('#featured-track').boundingBox();
      await page.mouse.move(box.x+box.width*.7,box.y+100);
      await page.mouse.down();
      await page.mouse.move(box.x+box.width*.2,box.y+100,{steps:15});
      await page.mouse.up(); await delay(700);
      assert.ok(await page.locator('#featured-track').evaluate(el=>el.scrollLeft)>0);
      assert.equal(new URL(page.url()).pathname,'/');
      await page.close();
    });
    await check('scrolling through the three steps updates the sticky illustration', async () => {
      const page=await newPage();
      for(const index of [0,1,2]) {
        await page.locator(`[data-step="${index}"]`).evaluate(el=>el.scrollIntoView({block:'center',behavior:'instant'}));
        await delay(650);
        assert.equal(await page.locator(`[data-scene="${index}"]`).evaluate(el=>el.classList.contains('is-active')),true);
      }
      await page.screenshot({path:path.join(resultsRoot,'journey.png')});
      await page.close();
    });
    await check('touch gallery swipes horizontally and still allows vertical page scrolling', async () => {
      const page=await newPage(Array.from({length:6},(_,i)=>({...event,id:i+1})),{viewport:{width:375,height:812},hasTouch:true,isMobile:true});
      await page.locator('#featured-track').scrollIntoViewIfNeeded();
      const box=await page.locator('#featured-track').boundingBox();
      const y=Math.min(600,Math.max(160,box.y+120));
      const client=await page.context().newCDPSession(page);
      await client.send('Input.dispatchTouchEvent',{type:'touchStart',touchPoints:[{x:310,y}]});
      for(let x=290;x>=70;x-=30) {
        await client.send('Input.dispatchTouchEvent',{type:'touchMove',touchPoints:[{x,y}]});
        await delay(25);
      }
      await client.send('Input.dispatchTouchEvent',{type:'touchEnd',touchPoints:[]});
      await delay(450);
      assert.ok(await page.locator('#featured-track').evaluate(el=>el.scrollLeft)>0,'horizontal gallery swipe');
      const top=await page.evaluate(()=>scrollY);
      await client.send('Input.dispatchTouchEvent',{type:'touchStart',touchPoints:[{x:180,y}]});
      for(let currentY=y-20;currentY>=y-140;currentY-=20) {
        await client.send('Input.dispatchTouchEvent',{type:'touchMove',touchPoints:[{x:180,y:currentY}]});
        await delay(25);
      }
      await client.send('Input.dispatchTouchEvent',{type:'touchEnd',touchPoints:[]});
      await delay(350);
      assert.ok(await page.evaluate(()=>scrollY)>top+20,'vertical swipe starting in gallery');
      assert.equal(new URL(page.url()).pathname,'/');
      await client.detach(); await page.close();
    });
    await check('reduced motion and missing GSAP keep content readable and functional', async () => {
      const page = await newPage([event],{reducedMotion:'reduce'});
      assert.equal(await page.locator('.hero-title').evaluate(el=>Number(getComputedStyle(el).opacity)),1);
      await page.close();
      const noMotion = await browser.newPage();
      await noMotion.route('**/*gsap*',route=>route.abort());
      await noMotion.route('**/*ScrollTrigger*',route=>route.abort());
      await noMotion.route('**/api/categories',route=>route.fulfill({json:categories}));
      await noMotion.route('**/api/events/public**',route=>route.fulfill({json:[event]}));
      await noMotion.goto(base,{waitUntil:'networkidle'});
      assert.equal(await noMotion.locator('.hero-title').evaluate(el=>Number(getComputedStyle(el).opacity)),1);
      assert.equal(await noMotion.locator('#events-grid .event-card').count(),1);
      await noMotion.close();
    });
    await check('scroll text rises smoothly from the viewport bottom and gains continuous font weight', async () => {
      const page=await newPage();
      const target=page.locator('#journey-title [data-scroll-line]').first();
      assert.equal(await target.count(),1,'a readable line is available for scroll typography');
      const bounds=await page.evaluate(()=>{
        const trigger=ScrollTrigger.getAll().find(item=>item.trigger===document.getElementById('journey-title'));
        return trigger ? {start:trigger.start,end:trigger.end} : null;
      });
      assert.ok(bounds,'scroll typography trigger');
      const sample=async progress=>{
        await page.evaluate(y=>scrollTo({top:y,behavior:'instant'}),bounds.start+(bounds.end-bounds.start)*progress);
        await delay(900);
        return target.evaluate(el=>{
          const style=getComputedStyle(el);
          const trigger=ScrollTrigger.getAll().find(item=>item.trigger===document.getElementById('journey-title'));
          return {weight:Number(style.fontWeight),opacity:Number(style.opacity),y:new DOMMatrixReadOnly(style.transform).m42,scrollY,triggerStart:trigger.start,progress:trigger.progress};
        });
      };
      const early=await sample(0),mid=await sample(.45),late=await sample(1);
      assert.ok(early.weight<=410 && early.y>10,JSON.stringify({early,mid,late}));
      assert.ok(mid.weight>early.weight+40 && mid.weight<late.weight-30,'weight has an intermediate value');
      assert.ok(late.weight>=740 && Math.abs(late.y)<1 && late.opacity>.99,'final text is bold and fully visible');
      await page.screenshot({path:path.join(resultsRoot,'scroll-text-bold.png')});
      const reversed=await sample(.2);
      assert.ok(reversed.weight<late.weight-100 && reversed.y>late.y,'scrolling back reverses smoothly');
      await page.screenshot({path:path.join(resultsRoot,'scroll-text.png')});
      await page.close();
    });
    await check('scroll text is immediately bold and readable with reduced motion', async () => {
      const page=await newPage([event],{reducedMotion:'reduce'});
      const lines=page.locator('[data-scroll-line]');
      assert.ok(await lines.count()>=4);
      assert.ok(await lines.evaluateAll(elements=>elements.every(el=>{
        const style=getComputedStyle(el);
        return Number(style.fontWeight)>=700 && Number(style.opacity)===1 && (style.transform==='none' || new DOMMatrixReadOnly(style.transform).m42===0);
      })));
      await page.close();
    });
    await check('GSAP hover animates imagery and arrows and restores their resting state', async () => {
      const page=await newPage();
      const card=page.locator('#featured-track .featured-card').first();
      await card.scrollIntoViewIfNeeded();
      const image=card.locator('img');
      const scale=()=>image.evaluate(el=>new DOMMatrixReadOnly(getComputedStyle(el).transform).a);
      const before=await scale();
      await card.hover(); await delay(600);
      const after=await scale();
      const detail=await page.evaluate(()=>({fine:matchMedia('(pointer: fine)').matches,hover:matchMedia('(hover: hover)').matches,style:document.querySelector('.featured-art img').getAttribute('style')}));
      assert.ok(after>before+.015,'image zoom on pointer hover '+JSON.stringify({before,after,...detail}));
      await page.mouse.move(0,0); await delay(650);
      assert.ok(Math.abs(await scale()-before)<.01,'image returns on pointer leave');
      const button=page.locator('.hero-actions .home-button');
      await button.scrollIntoViewIfNeeded();
      await button.focus(); await delay(450);
      assert.ok(await button.locator('.icon').evaluate(el=>new DOMMatrixReadOnly(getComputedStyle(el).transform).m41)>2,'keyboard focus gets arrow feedback');
      await page.close();
    });
    await check('variable-weight text remains completely visible on narrow screens', async () => {
      const page=await newPage([event],{reducedMotion:'reduce'});
      for(const width of [320,375,768,1024,1440]) {
        await page.setViewportSize({width,height:900});
        const overflowing=await page.locator('[data-scroll-line]').evaluateAll(lines=>lines.filter(el=>el.scrollWidth>el.clientWidth+1).map(el=>({text:el.textContent,width:el.clientWidth,content:el.scrollWidth})));
        assert.deepEqual(overflowing,[],`clipped text at ${width}px`);
      }
      await page.close();
    });
    await check('back-forward cache restores do not multiply scroll animation contexts', async () => {
      const page=await newPage();
      const counts=await page.evaluate(()=>{
        const counts=[];
        for(let i=0;i<4;i++) {
          dispatchEvent(new PageTransitionEvent('pagehide',{persisted:true}));
          dispatchEvent(new PageTransitionEvent('pageshow',{persisted:true}));
          counts.push(ScrollTrigger.getAll().length);
        }
        return counts;
      });
      assert.ok(counts.every(count=>count===counts[0]),`animation contexts grow: ${counts.join(',')}`);
      await page.close();
    });
    assert.deepEqual(errors,[], 'browser JavaScript errors');
    console.log(`Homepage checks: ${checks} passed; screenshots: ${resultsRoot}`);
  } finally { await browser.close(); await new Promise(resolve=>server.close(resolve)); }
}
main().catch(error=>{console.error(error);process.exitCode=1;});
