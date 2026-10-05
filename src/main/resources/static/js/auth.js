// ====================================================================
// AUTHENTICATION & SESSION MANAGEMENT
// ====================================================================

function getToken() {
    return localStorage.getItem('token');
}

function getUser() {
    const userStr = localStorage.getItem('user');
    try {
        return userStr ? JSON.parse(userStr) : null;
    } catch {
        return null;
    }
}

function saveAuth(token, user) {
    localStorage.setItem('token', token);
    localStorage.setItem('user', JSON.stringify(user));
}

function logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    window.location.href = '/login.html';
}

function requireAuth(allowedRoles = []) {
    const token = getToken();
    const user = getUser();

    if (!token || !user) {
        window.location.href = '/login.html?redirect=' + encodeURIComponent(window.location.pathname);
        return false;
    }

    if (allowedRoles.length > 0 && !allowedRoles.includes(user.role)) {
        alert('Bạn không có quyền truy cập trang này!');
        if (user.role === 'ROLE_ADMIN') {
            window.location.href = '/admin/index.html';
        } else if (user.role === 'ROLE_ORGANIZER') {
            window.location.href = '/organizer/index.html';
        } else {
            window.location.href = '/';
        }
        return false;
    }

    return true;
}

// Render dynamic navbar based on user session
function setupNavbar() {
    const navAuth = document.getElementById('nav-auth');
    if (!navAuth) return;

    const user = getUser();
    if (user) {
        let dashboardLink = '';
        if (user.role === 'ROLE_ADMIN') {
            dashboardLink = `<a href="/admin/index.html" class="btn btn-outline btn-sm">Quản Trị Admin</a>`;
        } else if (user.role === 'ROLE_ORGANIZER') {
            dashboardLink = `<a href="/organizer/index.html" class="btn btn-outline btn-sm">Kênh Ban Tổ Chức</a>`;
        }

        const isAdmin = user.role === 'ROLE_ADMIN' || user.role === 'ADMIN';
        const isOrganizer = user.role === 'ROLE_ORGANIZER' || user.role === 'ORGANIZER';
        
        let attendeeLinks = '';
        if (!isAdmin && !isOrganizer) {
            const path = window.location.pathname;
            const isHome = path === '/' || path === '/index.html' || path.endsWith('/index.html') || path === '';
            const isRegEvents = path.includes('registered-events');
            const isTickets = path.includes('my-tickets');
            
            attendeeLinks = `
                <a href="/" class="nav-link ${isHome ? 'active' : ''}" style="font-weight:600; text-decoration:none; padding:0.4rem 0.75rem; border-radius:6px; transition:all 0.2s; color:${isHome ? 'var(--primary)' : 'var(--text-secondary)'}; background:${isHome ? 'var(--primary-light)' : 'transparent'};">
                    Trang chủ
                </a>
                <a href="/registered-events.html" class="nav-link ${isRegEvents ? 'active' : ''}" style="font-weight:600; text-decoration:none; padding:0.4rem 0.75rem; border-radius:6px; transition:all 0.2s; color:${isRegEvents ? 'var(--primary)' : 'var(--text-secondary)'}; background:${isRegEvents ? 'var(--primary-light)' : 'transparent'};">
                    Sự kiện đã đăng ký
                </a>
                <a href="/my-tickets.html" class="nav-link ${isTickets ? 'active' : ''}" style="font-weight:600; text-decoration:none; padding:0.4rem 0.75rem; border-radius:6px; transition:all 0.2s; color:${isTickets ? 'var(--primary)' : 'var(--text-secondary)'}; background:${isTickets ? 'var(--primary-light)' : 'transparent'};">
                    Vé của tôi
                </a>
            `;
        }

        let profileUrl = '/profile.html';
        if (isOrganizer) profileUrl = '/organizer/profile.html';

        const profileNameLink = `
            <a href="${profileUrl}" class="user-profile-link" 
               style="font-size:0.875rem; font-weight:600; color:var(--text-primary); text-decoration:none; cursor:pointer; padding:0.3rem 0.6rem; border-radius:6px; transition:all 0.2s; display:inline-flex; align-items:center; gap:0.35rem;" 
               onmouseover="this.style.color='var(--primary)'; this.style.backgroundColor='var(--primary-light)'" 
               onmouseout="this.style.color='var(--text-primary)'; this.style.backgroundColor='transparent'" 
               title="Nhấp để xem và sửa thông tin cá nhân">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="opacity:0.75;"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle></svg>
                ${user.fullName}
            </a>
        `;

        navAuth.innerHTML = `
            ${dashboardLink}
            ${attendeeLinks}
            <div style="display:flex; align-items:center; gap:0.5rem; margin-left:0.5rem;">
                ${profileNameLink}
                <button onclick="logout()" class="btn btn-outline btn-sm" style="padding:0.3rem 0.6rem;">Đăng xuất</button>
            </div>
        `;
    } else {
        navAuth.innerHTML = `
            <a href="/login.html" class="btn btn-outline btn-sm">Đăng nhập</a>
            <a href="/register.html" class="btn btn-primary btn-sm">Đăng ký tham gia</a>
        `;
    }
}

document.addEventListener('DOMContentLoaded', setupNavbar);
