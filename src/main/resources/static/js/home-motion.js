/* Optional GSAP layer: native page functionality never depends on this module. */
(() => {
    'use strict';
    let media, cardContext, refreshTimer;
    const revealed = new WeakSet();
    const hovered = new WeakSet();
    const canAnimate = () => window.gsap && window.ScrollTrigger && !window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    function reveal(root = document) {
        if (!canAnimate() || !media) return;
        root.querySelectorAll('[data-reveal]').forEach(element => {
            if (revealed.has(element) || !element.getClientRects().length) return;
            revealed.add(element);
            media.add('(prefers-reduced-motion: no-preference)', () => {
                gsap.from(element, {
                    y: 20, opacity: 0, duration: .55, ease: 'power2.out',
                    scrollTrigger: {trigger: element, start: 'top 85%', once: true},
                    clearProps: 'transform,opacity'
                });
            });
        });
        hovers(root, media);
    }
    function scrollTypography() {
        media.add('(prefers-reduced-motion: no-preference)', () => {
            document.querySelectorAll('[data-scroll-text]').forEach(block => {
                const lines = block.querySelectorAll('[data-scroll-line]');
                // Establish an explicit start state so a refresh at progress zero
                // cannot flash the final, CSS-defined bold state.
                gsap.set(lines, {yPercent: 92, opacity: .16, fontWeight: 400});
                gsap.to(lines, {
                    yPercent: 0, opacity: 1, fontWeight: 750,
                    duration: 1, stagger: .14, ease: 'none',
                    scrollTrigger: {
                        trigger: block,
                        start: 'clamp(top 96%)', end: 'clamp(top 52%)',
                        scrub: .65, invalidateOnRefresh: true
                    }
                });
            });
        });
    }
    function hovers(root, owner) {
        if (!owner) return;
        owner.add('(hover: hover) and (pointer: fine) and (prefers-reduced-motion: no-preference)', () => {
            const cleanups = [];
            root.querySelectorAll('.home-button, .text-link, .event-detail-link, .featured-card, .event-card').forEach(element => {
                if (hovered.has(element)) return;
                const card = element.matches('.featured-card, .event-card');
                const target = element.querySelector(card ? 'img' : '.icon');
                if (!target) return;
                hovered.add(element);
                const options = {duration: card ? .55 : .35, ease: 'power3.out'};
                const animateX = gsap.quickTo(target, card ? 'scaleX' : 'x', options);
                const animateY = card ? gsap.quickTo(target, 'scaleY', options) : null;
                const animate = value => { animateX(value); animateY?.(value); };
                let pointerInside = false, focusInside = false;
                const update = () => animate(pointerInside || focusInside ? (card ? 1.028 : 5) : (card ? 1 : 0));
                const enter = event => { if (event.pointerType !== 'touch') { pointerInside = true; update(); } };
                const leave = () => { pointerInside = false; update(); };
                const focus = () => { focusInside = true; update(); };
                const blur = event => { if (!element.contains(event.relatedTarget)) { focusInside = false; update(); } };
                element.addEventListener('pointerenter', enter);
                element.addEventListener('pointerleave', leave);
                element.addEventListener('focusin', focus);
                element.addEventListener('focusout', blur);
                cleanups.push(() => {
                    element.removeEventListener('pointerenter', enter);
                    element.removeEventListener('pointerleave', leave);
                    element.removeEventListener('focusin', focus);
                    element.removeEventListener('focusout', blur);
                    hovered.delete(element);
                });
            });
            return () => cleanups.forEach(cleanup => cleanup());
        });
    }
    function dispose() {
        clearCards();
        media?.revert();
        media = null;
        clearTimeout(refreshTimer);
    }
    function init({ restored = false } = {}) {
        dispose();
        if (!window.gsap || !window.ScrollTrigger) return;
        gsap.registerPlugin(ScrollTrigger);
        media = gsap.matchMedia();
        scrollTypography();
        if (!restored) {
            media.add('(prefers-reduced-motion: no-preference)', () => {
                gsap.from('.hero-enter', { y: 22, opacity: 0, duration: .48, stagger: .055, ease: 'power2.out', clearProps: 'transform,opacity' });
            });
        }
        media.add('(min-width: 1024px) and (prefers-reduced-motion: no-preference)', () => {
            const panels = [...document.querySelectorAll('[data-scene]')];
            const counter = document.getElementById('journey-counter');
            let selected = -1;
            const setScene = index => {
                if (index === selected) return;
                selected = index;
                panels.forEach((panel, i) => panel.classList.toggle('is-active', i === index));
                counter.textContent = `0${index + 1} / 03`;
            };
            gsap.fromTo('#journey-progress-fill', {scaleX: 1 / 3}, {
                scaleX: 1, ease: 'none', scrollTrigger: {
                    trigger: '.journey-steps', start: 'top 45%', end: 'bottom 55%', scrub: .4,
                    onUpdate: self => setScene(Math.min(2, Math.floor(self.progress * 3)))
                }
            });
            return () => { setScene(0); };
        });
        reveal();
        if (document.fonts) document.fonts.ready.then(refresh);
    }
    function clearCards() { cardContext?.revert(); cardContext = null; }
    function cards(root) {
        clearCards();
        if (!canAnimate()) return;
        // from() leaves all content visible if GSAP is unavailable. Only new cards get triggers.
        cardContext = gsap.matchMedia();
        cardContext.add('(prefers-reduced-motion: no-preference)', () => {
            root.querySelectorAll('.event-card').forEach((card, index) => {
                gsap.from(card, {
                    y: 20, opacity: 0, duration: .45, delay: (index % 3) * .06,
                    ease: 'power2.out', clearProps: 'transform,opacity',
                    scrollTrigger: {trigger: card, start: 'top 88%', once: true}
                });
            });
        });
        hovers(root, cardContext);
    }
    function menu(element) {
        if (!canAnimate()) return;
        gsap.fromTo(element.querySelectorAll('.home-nav a, .home-auth > *'),
            {opacity: 0, y: 10}, {opacity: 1, y: 0, duration: .22, stagger: .035, ease: 'power2.out', overwrite: true, clearProps: 'transform,opacity'});
    }
    function refresh() {
        clearTimeout(refreshTimer);
        if (window.ScrollTrigger) refreshTimer = setTimeout(() => ScrollTrigger.refresh(), 100);
    }
    window.addEventListener('pagehide', dispose);
    window.addEventListener('pageshow', event => { if (event.persisted) init({ restored: true }); });
    window.HomeMotion = {init, reveal, cards, clearCards, menu, refresh};
})();
