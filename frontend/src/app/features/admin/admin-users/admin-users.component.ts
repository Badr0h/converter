import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService } from '../../../core/services/admin.service';
import { UserResponseDto } from '../../../core/models/user.model';
import { ConversionResponseDto } from '../../../core/models/conversion.model';

@Component({
    selector: 'app-admin-users',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './admin-users.component.html',
    styleUrls: ['./admin-users.component.scss']
})
export class AdminUsersComponent implements OnInit {
    users: UserResponseDto[] = [];
    filteredUsers: UserResponseDto[] = [];
    paginatedUsers: UserResponseDto[] = [];
    loading = true;

    // Filters
    searchQuery = '';
    roleFilter = '';
    statusFilter = '';

    // Sort
    sortField = 'id';
    sortDir: 'asc' | 'desc' = 'asc';

    // Pagination
    currentPage = 0;
    pageSize = 10;
    get totalPages(): number { return Math.ceil(this.filteredUsers.length / this.pageSize); }

    // Modals
    showCreateModal = false;
    newUser = { fullName: '', email: '', password: '', role: 'USER' };

    showConversionsModal = false;
    selectedUser: UserResponseDto | null = null;
    userConversions: ConversionResponseDto[] = [];
    userConversionsLoading = false;

    showDeleteModal = false;
    userToDelete: UserResponseDto | null = null;

    constructor(private adminService: AdminService) { }

    ngOnInit(): void {
        this.adminService.getAllUsers().subscribe({
            next: (users) => {
                this.users = users;
                this.applyFilters();
                this.loading = false;
            },
            error: () => { this.loading = false; }
        });
    }

    applyFilters(): void {
        let result = [...this.users];

        if (this.searchQuery) {
            const q = this.searchQuery.toLowerCase();
            result = result.filter(u =>
                u.email.toLowerCase().includes(q) ||
                u.fullName.toLowerCase().includes(q)
            );
        }
        if (this.roleFilter) {
            result = result.filter(u => u.role === this.roleFilter);
        }
        if (this.statusFilter === 'enabled') {
            result = result.filter(u => u.enabled);
        } else if (this.statusFilter === 'disabled') {
            result = result.filter(u => !u.enabled);
        }

        // Sort
        result.sort((a, b) => {
            const av = (a as any)[this.sortField] ?? '';
            const bv = (b as any)[this.sortField] ?? '';
            const cmp = av < bv ? -1 : av > bv ? 1 : 0;
            return this.sortDir === 'asc' ? cmp : -cmp;
        });

        this.filteredUsers = result;
        this.currentPage = 0;
        this.updatePagination();
    }

    sortBy(field: string): void {
        if (this.sortField === field) {
            this.sortDir = this.sortDir === 'asc' ? 'desc' : 'asc';
        } else {
            this.sortField = field;
            this.sortDir = 'asc';
        }
        this.applyFilters();
    }

    getSortIcon(field: string): string {
        if (this.sortField !== field) return '↕';
        return this.sortDir === 'asc' ? '↑' : '↓';
    }

    updatePagination(): void {
        const start = this.currentPage * this.pageSize;
        this.paginatedUsers = this.filteredUsers.slice(start, start + this.pageSize);
    }

    prevPage(): void {
        if (this.currentPage > 0) { this.currentPage--; this.updatePagination(); }
    }

    nextPage(): void {
        if (this.currentPage < this.totalPages - 1) { this.currentPage++; this.updatePagination(); }
    }

    onPageSizeChange(): void { this.currentPage = 0; this.updatePagination(); }

    openCreateModal(): void {
        this.showCreateModal = true;
        this.newUser = { fullName: '', email: '', password: '', role: 'USER' };
    }

    closeCreateModal(): void {
        this.showCreateModal = false;
    }

    createUser(): void {
        if (!this.newUser.fullName || !this.newUser.email || !this.newUser.password) return;
        this.adminService.createUser(this.newUser).subscribe({
            next: (user) => {
                this.users.unshift(user);
                this.applyFilters();
                this.closeCreateModal();
            },
            error: () => {
                alert('Could not create user. Check if email is available.');
            }
        });
    }

    changeRole(user: UserResponseDto, event: Event): void {
        const role = (event.target as HTMLSelectElement).value;
        this.adminService.updateUserRole(user.id, role).subscribe({
            next: (updated) => {
                const idx = this.users.findIndex(u => u.id === user.id);
                if (idx >= 0) this.users[idx] = updated;
                this.applyFilters();
            }
        });
    }

    toggleUser(user: UserResponseDto): void {
        this.adminService.toggleUserEnabled(user.id).subscribe({
            next: (updated) => {
                const idx = this.users.findIndex(u => u.id === user.id);
                if (idx >= 0) this.users[idx] = updated;
                this.applyFilters();
            }
        });
    }

    viewUserConversions(user: UserResponseDto): void {
        this.selectedUser = user;
        this.showConversionsModal = true;
        this.userConversionsLoading = true;
        this.adminService.getUserConversions(user.id).subscribe({
            next: (c) => { this.userConversions = c; this.userConversionsLoading = false; },
            error: () => { this.userConversionsLoading = false; }
        });
    }

    deleteUser(user: UserResponseDto): void {
        this.userToDelete = user;
        this.showDeleteModal = true;
    }

    confirmDelete(): void {
        if (!this.userToDelete) return;
        this.adminService.deleteUser(this.userToDelete.id).subscribe({
            next: () => {
                this.users = this.users.filter(u => u.id !== this.userToDelete!.id);
                this.applyFilters();
                this.cancelDelete();
            }
        });
    }

    cancelDelete(): void {
        this.userToDelete = null;
        this.showDeleteModal = false;
    }

    closeModal(): void {
        this.showConversionsModal = false;
        this.selectedUser = null;
        this.userConversions = [];
    }

    formatDate(date: Date | string): string {
        const d = typeof date === 'string' ? new Date(date) : date;
        return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
    }
}
