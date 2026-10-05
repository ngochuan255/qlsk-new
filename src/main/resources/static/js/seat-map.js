// ====================================================================
// INTERACTIVE SEAT MAP PICKER
// ====================================================================

class SeatPicker {
    constructor(containerId, onSeatSelected) {
        this.container = document.getElementById(containerId);
        this.onSeatSelected = onSeatSelected;
        this.selectedSeat = null;
        this.seats = [];
    }

    render(seats) {
        this.seats = seats;
        if (!this.container) return;
        this.container.innerHTML = '';

        if (!seats || seats.length === 0) {
            this.container.innerHTML = '<p style="color:var(--text-muted); padding:1rem;">Sự kiện này không áp dụng sơ đồ chỗ ngồi cố định.</p>';
            return;
        }

        // Group seats by row
        const rowMap = new Map();
        seats.forEach(seat => {
            if (!rowMap.has(seat.seatRow)) {
                rowMap.set(seat.seatRow, []);
            }
            rowMap.get(seat.seatRow).push(seat);
        });

        const grid = document.createElement('div');
        grid.className = 'seat-grid';

        const sortedRows = Array.from(rowMap.keys()).sort((a, b) => a - b);
        sortedRows.forEach(rowNum => {
            const rowSeats = rowMap.get(rowNum).sort((a, b) => a.seatCol - b.seatCol);
            const rowElem = document.createElement('div');
            rowElem.className = 'seat-row';

            const rowChar = String.fromCharCode(64 + rowNum);
            const labelElem = document.createElement('div');
            labelElem.className = 'row-label';
            labelElem.textContent = rowChar;
            rowElem.appendChild(labelElem);

            rowSeats.forEach(seat => {
                const seatElem = document.createElement('div');
                seatElem.className = `seat ${seat.seatType === 'VIP' ? 'seat-vip' : 'seat-standard'}`;
                seatElem.textContent = seat.seatCode;
                seatElem.dataset.seatId = seat.id;

                if (seat.isBooked) {
                    seatElem.classList.add('seat-booked');
                    seatElem.title = `Ghế ${seat.seatCode} (Đã có người đặt)`;
                } else {
                    seatElem.title = `Ghế ${seat.seatCode} (${seat.seatType}) - Còn trống`;
                    seatElem.addEventListener('click', () => this.selectSeat(seat, seatElem));
                }

                rowElem.appendChild(seatElem);
            });

            grid.appendChild(rowElem);
        });

        this.container.appendChild(grid);
    }

    selectSeat(seat, element) {
        if (seat.isBooked) return;

        // Deselect previous
        const prev = this.container.querySelector('.seat-selected');
        if (prev) {
            prev.classList.remove('seat-selected');
        }

        // If clicking same seat again, unselect
        if (this.selectedSeat && this.selectedSeat.id === seat.id) {
            this.selectedSeat = null;
            if (this.onSeatSelected) this.onSeatSelected(null);
            return;
        }

        this.selectedSeat = seat;
        element.classList.add('seat-selected');
        if (this.onSeatSelected) this.onSeatSelected(seat);
    }

    getSelectedSeat() {
        return this.selectedSeat;
    }
}
