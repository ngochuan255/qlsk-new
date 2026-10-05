// ====================================================================
// API UTILITIES & HTTP WRAPPER WITH JWT AUTH
// ====================================================================

const API_BASE = '/api';

async function apiCall(endpoint, method = 'GET', data = null) {
    const token = localStorage.getItem('token');
    const headers = {
        'Content-Type': 'application/json',
    };

    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }

    const options = {
        method: method,
        headers: headers,
    };

    if (data && (method === 'POST' || method === 'PUT' || method === 'PATCH')) {
        options.body = JSON.stringify(data);
    }

    try {
        const response = await fetch(`${API_BASE}${endpoint}`, options);
        
        let result = null;
        try {
            result = await response.json();
        } catch (e) {
            // No json body
        }

        if (response.status === 401) {
            // Nếu gọi API đăng nhập, hiển thị thông báo sai tài khoản/mật khẩu
            if (endpoint.includes('/auth/login')) {
                throw new Error((result && result.message) ? result.message : 'Tên đăng nhập hoặc mật khẩu không chính xác');
            }

            localStorage.removeItem('token');
            localStorage.removeItem('user');
            if (!window.location.pathname.includes('login.html')) {
                window.location.href = '/login.html?expired=true';
            }
            throw new Error((result && result.message) ? result.message : 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
        }

        if (!response.ok) {
            throw new Error((result && result.message) ? result.message : 'Đã xảy ra lỗi trong quá trình xử lý');
        }

        return result;
    } catch (error) {
        console.error('API Error:', error);
        throw error;
    }
}

// Toast Notifications
function showToast(message, type = 'info') {
    let container = document.getElementById('toast-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toast-container';
        document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.innerHTML = `
        <div style="display:flex; justify-content:space-between; align-items:center;">
            <span>${message}</span>
            <button onclick="this.parentElement.parentElement.remove()" style="background:none;border:none;cursor:pointer;font-weight:bold;margin-left:8px;">&times;</button>
        </div>
    `;

    container.appendChild(toast);
    setTimeout(() => {
        if (toast.parentElement) toast.remove();
    }, 4000);
}

// Formatters
function formatCurrency(amount) {
    if (amount == null) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
}

function formatDateTime(dateStr) {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    return d.toLocaleString('vi-VN', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}
