// Server metadata includes all registration history, including cancelled tickets.
function getEventMutationBlockReason(event) {
    if (!event) return 'Không tìm thấy sự kiện';
    const start = new Date(event.startTime);
    if (!Number.isFinite(start.getTime()) || start.getTime() <= Date.now()
        || event.status === 'ONGOING' || event.status === 'COMPLETED') {
        return 'Không thể sửa hoặc xóa sự kiện đã bắt đầu hoặc đã kết thúc';
    }
    if (event.canModify !== true) {
        return event.modificationBlockedReason
            || 'Chỉ được sửa hoặc xóa khi sự kiện chưa diễn ra và chưa có người đăng ký';
    }
    return '';
}
