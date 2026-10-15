/* Homepage data and native interactions. Animation is an optional enhancement. */
(() => {
    'use strict';
    const $ = id => document.getElementById(id);
    const statusLabels = {
        OPEN_FOR_REGISTRATION: 'Đang mở đăng ký', FULL: 'Đã đủ chỗ',
        ONGOING: 'Đang diễn ra', COMPLETED: 'Đã kết thúc', CANCELLED: 'Đã hủy',
        APPROVED: 'Đã duyệt', DRAFT: 'Bản nháp'
    };
    const reducedMotion = () => window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    const scrollBehavior = () => reducedMotion() ? 'instant' : 'smooth';
    const node = (tag, className, text) => {
        const element = document.createElement(tag);
        if (className) element.className = className;
        if (text !== undefined && text !== null) element.textContent = String(text);
        return element;
    };
    const icon = name => {
        const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
        svg.classList.add('icon');
        svg.setAttribute('aria-hidden', 'true');
        const use = document.createElementNS('http://www.w3.org/2000/svg', 'use');
        use.setAttribute('href', '#i-' + name);
        svg.append(use);
        return svg;
    };
    function safeImageUrl(value) {
        if (!value || typeof value !== 'string') return null;
        try {
            const url = new URL(value, window.location.origin);
            return ['http:', 'https:'].includes(url.protocol) ? url.href : null;
        } catch { return null; }
    }
    function imageFallback() {
        const fallback = node('div', 'image-fallback');
        fallback.append(icon('ticket'), node('span', '', 'EVENTHUB / EXPERIENCE'));
        fallback.setAttribute('aria-hidden', 'true');
        return fallback;
    }
    function imageFor(event, eager = false) {
        const url = safeImageUrl(event.bannerUrl);
        if (!url) return imageFallback();
        const image = node('img');
        image.alt = 'Poster: ' + (event.title || 'Sự kiện');
        image.width = 1200;
        image.height = 632;
        image.loading = eager ? 'eager' : 'lazy';
        image.decoding = 'async';
        image.draggable = false;
        if (eager) image.setAttribute('fetchpriority', 'high');
        image.addEventListener('error', () => {
            image.replaceWith(imageFallback());
            window.HomeMotion?.refresh();
        }, { once: true });
        image.addEventListener('load', () => window.HomeMotion?.refresh(), { once: true });
        image.src = url;
        return image;
    }
    function statusBadge(event) {
        const badge = node('span', 'status-badge', statusLabels[event.status] || event.statusText || 'Xem thông tin');
        badge.dataset.status = event.status || '';
        return badge;
    }
    function detailLink(event, className, label = 'Xem chi tiết') {
        const link = node('a', className, label);
        link.href = '/event-detail.html?id=' + encodeURIComponent(String(event.id));
        link.setAttribute('aria-label', label + ': ' + (event.title || 'Sự kiện'));
        link.append(icon('arrow'));
        return link;
    }
    function eventDate(value) {
        if (!value) return 'Thời gian sẽ được cập nhật';
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) return 'Thời gian sẽ được cập nhật';
        return new Intl.DateTimeFormat('vi-VN', {
            day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit'
        }).format(date);
    }
    function metaLine(name, text) {
        const line = node('span');
        line.append(icon(name), node('span', '', text));
        return line;
    }
    function metadata(event, compact = false) {
        const meta = node('div', 'event-meta');
        meta.append(metaLine('calendar', eventDate(event.startTime)), metaLine('pin', event.location || 'Địa điểm sẽ được cập nhật'));
        if (!compact && Number.isFinite(event.remainingSeats) && Number.isFinite(event.maxAttendees)) {
            meta.append(metaLine('users', `${event.remainingSeats} / ${event.maxAttendees} chỗ còn lại`));
        }
        if (!compact && Number.isFinite(event.averageRating) && event.averageRating > 0 && event.feedbackCount > 0) {
            meta.append(metaLine('star', `${event.averageRating} / 5 · ${event.feedbackCount} đánh giá`));
        }
        return meta;
    }
    function eventCard(event, featured = false) {
        const card = node('article', featured ? 'featured-card' : 'event-card');
        const art = node('div', featured ? 'featured-art' : 'event-banner');
        art.append(imageFor(event));
        const content = node('div', featured ? 'featured-info' : 'event-content');
        content.append(node('span', 'event-category', event.categoryName || 'Khám phá sự kiện'),
            node('h3', featured ? '' : 'event-title', event.title || 'Sự kiện'), metadata(event, featured));
        if (featured) {
            const badge = statusBadge(event);
            badge.style.marginTop = '20px';
            content.append(badge, detailLink(event, 'home-button button-light'));
        } else {
            const footer = node('div', 'event-footer');
            footer.append(statusBadge(event), detailLink(event, 'event-detail-link'));
            content.append(footer);
        }
        card.append(art, content);
        return card;
    }
    function renderHero(event) {
        if (!event) return;
        $('hero-media').replaceChildren(imageFor(event, true));
        $('hero-event-state').textContent = (statusLabels[event.status] || 'SỰ KIỆN TIÊU ĐIỂM').toLocaleUpperCase('vi');
        const category = node('span', 'caption-label', event.categoryName || 'SỰ KIỆN TIÊU ĐIỂM');
        const title = node('p');
        const link = node('a', '', event.title || 'Sự kiện');
        link.href = '/event-detail.html?id=' + encodeURIComponent(String(event.id));
        title.append(link);
        const note = node('div', 'caption-note');
        note.append(metaLine('calendar', eventDate(event.startTime)), metaLine('pin', event.location || 'Địa điểm sẽ được cập nhật'));
        $('hero-caption').replaceChildren(category, title, note);
    }
    function renderFeatured(events) {
        const rank = { OPEN_FOR_REGISTRATION: 0, ONGOING: 1, FULL: 2 };
        const selected = events.filter(event => Object.hasOwn(rank, event.status))
            .sort((a, b) => rank[a.status] - rank[b.status]).slice(0, 6);
        $('featured-section').hidden = selected.length === 0;
        if (!selected.length) return;
        renderHero(selected[0]);
        const track = $('featured-track');
        track.replaceChildren(...selected.map(event => eventCard(event, true)));
        track.dataset.layout = selected.length > 2 ? 'carousel' : selected.length === 2 ? 'pair' : 'single';
        const carousel = selected.length > 2;
        $('featured-controls').hidden = !carousel;
        $('gallery-hint').hidden = !carousel;
        if (carousel) {
            track.tabIndex = 0;
            track.setAttribute('role', 'region');
            track.setAttribute('aria-roledescription', 'carrousel');
            track.setAttribute('aria-describedby', 'gallery-hint');
            setupGallery(track);
        }
        window.HomeMotion?.reveal($('featured-section'));
        window.HomeMotion?.refresh();
    }
    function setupGallery(track) {
        let dragging = false, moved = false, startX = 0, startScroll = 0, pointer = null;
        let suppressClick = false, clickTimer = 0, frame = 0;
        const cards = [...track.children];
        const leftFor = card => card.offsetLeft - cards[0].offsetLeft;
        const currentIndex = () => cards.reduce((best, card, index) =>
            Math.abs(leftFor(card) - track.scrollLeft) < Math.abs(leftFor(cards[best]) - track.scrollLeft) ? index : best, 0);
        const update = () => {
            const end = track.scrollWidth - track.clientWidth;
            $('featured-prev').disabled = track.scrollLeft < 3;
            $('featured-next').disabled = track.scrollLeft >= end - 3;
            $('gallery-count').textContent = `${String(currentIndex() + 1).padStart(2, '0')} / ${String(cards.length).padStart(2, '0')}`;
            frame = 0;
        };
        const go = index => {
            const bounded = Math.max(0, Math.min(cards.length - 1, index));
            track.scrollTo({ left: leftFor(cards[bounded]), behavior: scrollBehavior() });
        };
        $('featured-prev').onclick = () => go(currentIndex() - 1);
        $('featured-next').onclick = () => go(currentIndex() + 1);
        track.addEventListener('scroll', () => { if (!frame) frame = requestAnimationFrame(update); }, { passive: true });
        track.addEventListener('keydown', event => {
            if (event.target !== track) return;
            if (!['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) return;
            event.preventDefault();
            go(event.key === 'Home' ? 0 : event.key === 'End' ? cards.length - 1 : currentIndex() + (event.key === 'ArrowRight' ? 1 : -1));
        });
        track.addEventListener('pointerdown', event => {
            // Touch retains the browser's own horizontal/vertical gesture arbitration.
            if (event.pointerType !== 'mouse' || event.button !== 0) return;
            dragging = true; moved = false; startX = event.clientX; startScroll = track.scrollLeft; pointer = event.pointerId;
        });
        track.addEventListener('pointermove', event => {
            if (!dragging || event.pointerId !== pointer) return;
            const distance = event.clientX - startX;
            if (!moved && Math.abs(distance) > 6) {
                moved = true;
                track.classList.add('is-dragging');
                track.setPointerCapture(pointer);
            }
            if (moved) { event.preventDefault(); track.scrollLeft = startScroll - distance; }
        });
        const finish = event => {
            if (!dragging || event.pointerId !== pointer) return;
            dragging = false;
            if (track.hasPointerCapture(pointer)) track.releasePointerCapture(pointer);
            track.classList.remove('is-dragging');
            if (moved) {
                suppressClick = true;
                clearTimeout(clickTimer);
                clickTimer = setTimeout(() => { suppressClick = false; }, 400);
                go(currentIndex());
            }
        };
        track.addEventListener('pointerup', finish);
        track.addEventListener('pointercancel', finish);
        track.addEventListener('pointerleave', event => { if (!moved) finish(event); });
        track.addEventListener('dragstart', event => event.preventDefault());
        track.addEventListener('click', event => {
            if (suppressClick) { event.preventDefault(); event.stopPropagation(); suppressClick = false; }
        }, true);
        new ResizeObserver(update).observe(track);
        update();
    }
    function setupMenu() {
        const menu = $('home-menu'), toggle = $('menu-toggle'), backdrop = $('menu-backdrop');
        const mobile = window.matchMedia('(max-width: 1023px)');
        let open = false, savedOverflow = '';
        const inertTargets = [document.querySelector('main'), document.querySelector('footer'), document.querySelector('.home-brand'), toggle];
        const close = (restore = true) => {
            if (!open) return;
            open = false;
            menu.classList.remove('is-open');
            menu.removeAttribute('role'); menu.removeAttribute('aria-modal'); menu.removeAttribute('aria-label');
            toggle.setAttribute('aria-expanded', 'false');
            backdrop.hidden = true;
            document.body.style.overflow = savedOverflow;
            inertTargets.forEach(target => { if (target) target.inert = false; });
            if (restore) toggle.focus({ preventScroll: true });
        };
        toggle.addEventListener('click', () => {
            if (open) return close();
            open = true; savedOverflow = document.body.style.overflow;
            menu.classList.add('is-open');
            menu.setAttribute('role', 'dialog'); menu.setAttribute('aria-modal', 'true'); menu.setAttribute('aria-label', 'Điều hướng');
            toggle.setAttribute('aria-expanded', 'true'); backdrop.hidden = false;
            document.body.style.overflow = 'hidden';
            inertTargets.forEach(target => { if (target) target.inert = true; });
            $('menu-close').focus();
            window.HomeMotion?.menu(menu);
        });
        $('menu-close').addEventListener('click', () => close());
        backdrop.addEventListener('click', () => close());
        menu.addEventListener('click', event => {
            if (mobile.matches && event.target.closest('a')) close();
        });
        menu.addEventListener('keydown', event => {
            if (!open) return;
            if (event.key === 'Escape') { event.preventDefault(); close(); return; }
            if (event.key !== 'Tab') return;
            const focusables = [...menu.querySelectorAll('a[href],button:not([disabled]),input,select,[tabindex="0"]')].filter(el => el.getClientRects().length);
            const first = focusables[0], last = focusables[focusables.length - 1];
            if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); }
            else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
        });
        mobile.addEventListener('change', () => close(false));
    }
    function setupAnchors() {
        document.addEventListener('click', event => {
            const link = event.target.closest('a[href^="#"]');
            if (!link || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return;
            const target = document.getElementById(link.getAttribute('href').slice(1));
            if (!target) return;
            event.preventDefault();
            target.scrollIntoView({ behavior: scrollBehavior(), block: 'start' });
            history.replaceState(null, '', link.getAttribute('href'));
            // Explicit anchor navigation moves keyboard focus; search submission does not.
            if (!target.hasAttribute('tabindex')) target.setAttribute('tabindex', '-1');
            target.focus({ preventScroll: true });
        });
    }
    function setupHeader() {
        let frame = 0;
        const update = () => { $('home-header').classList.toggle('is-scrolled', window.scrollY > 64); frame = 0; };
        window.addEventListener('scroll', () => { if (!frame) frame = requestAnimationFrame(update); }, { passive: true });
        update();
    }
    function setupAccountMenu() {
        // Reuse the shared session helpers, but render account data as text on this page.
        const target = $('home-auth');
        const user = getUser();
        const link = (href, label, className = 'btn btn-outline btn-sm') => {
            const anchor = node('a', className, label);
            anchor.href = href;
            return anchor;
        };
        if (!user) {
            target.replaceChildren(link('/login.html', 'Đăng nhập'), link('/register.html', 'Đăng ký tham gia', 'btn btn-primary btn-sm'));
            return;
        }
        const role = String(user.role || '').replace(/^ROLE_/, '');
        if (role === 'ADMIN') target.append(link('/admin/index.html', 'Quản trị Admin'));
        else if (role === 'ORGANIZER') target.append(link('/organizer/index.html', 'Kênh Ban tổ chức'));
        else target.append(link('/', 'Trang chủ', 'nav-link'), link('/registered-events.html', 'Sự kiện đã đăng ký', 'nav-link'), link('/my-tickets.html', 'Vé của tôi', 'nav-link'));
        const account = node('div', 'home-account');
        const profile = link(role === 'ORGANIZER' ? '/organizer/profile.html' : '/profile.html', user.fullName || 'Tài khoản', 'user-profile-link');
        profile.title = user.fullName || 'Tài khoản';
        const exit = node('button', 'btn btn-outline btn-sm', 'Đăng xuất');
        exit.type = 'button'; exit.addEventListener('click', logout);
        account.append(profile, exit);
        target.append(account);
    }
    let latestRequest = 0;
    let initialSucceeded = false;
    function filters() {
        return { keyword: $('keyword').value.trim(), categoryId: $('category-select').value, status: $('status-select').value };
    }
    function updateSummary(values) {
        const active = $('active-filters');
        active.replaceChildren();
        if (values.keyword) active.append(node('span', 'filter-chip', 'Từ khóa: ' + values.keyword));
        if (values.categoryId) active.append(node('span', 'filter-chip', $('category-select').selectedOptions[0]?.textContent || 'Danh mục đã chọn'));
        if (values.status) active.append(node('span', 'filter-chip', statusLabels[values.status] || values.status));
        $('filter-summary').hidden = !active.childElementCount;
    }
    function loading() {
        const grid = $('events-grid');
        const currentHeight = grid.getBoundingClientRect().height;
        if (currentHeight > 0) grid.style.minHeight = currentHeight + 'px';
        grid.setAttribute('aria-busy', 'true');
        const skeletons = Array.from({ length: 3 }, () => {
            const item = node('div', 'event-skeleton');
            item.setAttribute('aria-hidden', 'true');
            item.append(node('div'), node('span'), node('span'), node('span'));
            return item;
        });
        grid.replaceChildren(...skeletons);
        $('events-count').textContent = 'Đang tải sự kiện';
    }
    function message(title, description, buttonText, action) {
        const panel = node('div', 'catalogue-message');
        panel.append(icon('search'), node('h3', '', title), node('p', '', description));
        const button = node('button', 'home-button button-primary', buttonText);
        button.type = 'button'; button.addEventListener('click', action);
        panel.append(button);
        return panel;
    }
    async function loadEvents({ initial = false, scroll = false } = {}) {
        const request = ++latestRequest;
        const values = initial ? {keyword: '', categoryId: '', status: ''} : filters();
        const params = new URLSearchParams();
        for (const [key, value] of Object.entries(values)) if (value) params.set(key, value);
        window.HomeMotion?.clearCards();
        loading(); updateSummary(values);
        try {
            const events = await apiCall('/events/public' + (params.size ? '?' + params.toString() : ''));
            if (!Array.isArray(events)) throw new Error('Invalid event response');
            if (initial) { initialSucceeded = true; renderFeatured(events); }
            if (request !== latestRequest) return;
            const grid = $('events-grid');
            grid.replaceChildren(...(events.length ? events.map(event => eventCard(event)) : [
                message('Chưa có sự kiện phù hợp', 'Thử đổi từ khóa hoặc bỏ bớt điều kiện để khám phá thêm.', 'Xóa bộ lọc', clearFilters)
            ]));
            $('events-count').textContent = `${events.length} sự kiện`;
            window.HomeMotion?.cards(grid);
        } catch {
            if (request !== latestRequest) return;
            $('events-count').textContent = 'Chưa tải được sự kiện';
            $('events-grid').replaceChildren(message('Chưa thể tải sự kiện', 'Kết nối đang gián đoạn. Vui lòng thử lại sau ít phút.', 'Thử lại', () => loadEvents({initial: initial && !initialSucceeded})));
        } finally {
            if (request === latestRequest) {
                $('events-grid').setAttribute('aria-busy', 'false');
                $('events-grid').style.minHeight = '';
                window.HomeMotion?.refresh();
                if (scroll) $('events').scrollIntoView({ behavior: scrollBehavior(), block: 'start' });
            }
        }
    }
    function clearFilters() {
        $('search-form').reset();
        loadEvents({scroll: true});
    }
    async function loadCategories() {
        const select = $('category-select');
        $('category-notice').hidden = true;
        try {
            const categories = await apiCall('/categories');
            if (!Array.isArray(categories)) throw new Error('Invalid categories response');
            const current = select.value;
            const all = node('option', '', 'Tất cả danh mục'); all.value = '';
            select.replaceChildren(all, ...categories.map(category => {
                const option = node('option', '', category.name);
                option.value = category.id;
                return option;
            }));
            select.value = current;
        } catch { $('category-notice').hidden = false; }
    }
    document.addEventListener('DOMContentLoaded', () => {
        setupAccountMenu(); setupMenu(); setupAnchors(); setupHeader();
        window.HomeMotion?.init();
        $('search-form').addEventListener('submit', event => { event.preventDefault(); loadEvents({scroll: true}); });
        $('clear-filters').addEventListener('click', clearFilters);
        $('retry-categories').addEventListener('click', loadCategories);
        loadCategories();
        loadEvents({initial: true});
    });
})();
