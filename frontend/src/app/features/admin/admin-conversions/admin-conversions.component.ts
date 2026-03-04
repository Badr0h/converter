import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService } from '../../../core/services/admin.service';
import { ConversionResponseDto, Format } from '../../../core/models/conversion.model';

@Component({
    selector: 'app-admin-conversions',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './admin-conversions.component.html',
    styleUrls: ['./admin-conversions.component.scss']
})
export class AdminConversionsComponent implements OnInit {
    conversions: ConversionResponseDto[] = [];
    filteredConversions: ConversionResponseDto[] = [];
    paginatedConversions: ConversionResponseDto[] = [];
    loading = true;

    formats = Object.values(Format);
    searchQuery = '';
    inputFormatFilter = '';
    outputFormatFilter = '';

    sortField = 'createdAt';
    sortDir: 'asc' | 'desc' = 'desc';

    currentPage = 0;
    pageSize = 25;
    get totalPages(): number { return Math.ceil(this.filteredConversions.length / this.pageSize); }

    showDetailModal = false;
    selectedConversion: ConversionResponseDto | null = null;

    showDeleteModal = false;
    conversionToDelete: ConversionResponseDto | null = null;

    constructor(private adminService: AdminService) { }

    ngOnInit(): void {
        this.adminService.getAllConversions().subscribe({
            next: (data) => {
                this.conversions = data;
                this.applyFilters();
                this.loading = false;
            },
            error: () => { this.loading = false; }
        });
    }

    applyFilters(): void {
        let result = [...this.conversions];

        if (this.searchQuery) {
            const q = this.searchQuery.toLowerCase();
            result = result.filter(c =>
                (c.userEmail ?? '').toLowerCase().includes(q) ||
                c.prompt.toLowerCase().includes(q)
            );
        }
        if (this.inputFormatFilter) {
            result = result.filter(c => c.inputFormat === this.inputFormatFilter);
        }
        if (this.outputFormatFilter) {
            result = result.filter(c => c.outputFormat === this.outputFormatFilter);
        }

        result.sort((a, b) => {
            const av = (a as any)[this.sortField] ?? '';
            const bv = (b as any)[this.sortField] ?? '';
            const cmp = av < bv ? -1 : av > bv ? 1 : 0;
            return this.sortDir === 'asc' ? cmp : -cmp;
        });

        this.filteredConversions = result;
        this.currentPage = 0;
        this.updatePagination();
    }

    sortBy(field: string): void {
        if (this.sortField === field) { this.sortDir = this.sortDir === 'asc' ? 'desc' : 'asc'; }
        else { this.sortField = field; this.sortDir = 'asc'; }
        this.applyFilters();
    }

    getSortIcon(field: string): string {
        if (this.sortField !== field) return '↕';
        return this.sortDir === 'asc' ? '↑' : '↓';
    }

    updatePagination(): void {
        const start = this.currentPage * this.pageSize;
        this.paginatedConversions = this.filteredConversions.slice(start, start + this.pageSize);
    }

    prevPage(): void { if (this.currentPage > 0) { this.currentPage--; this.updatePagination(); } }
    nextPage(): void { if (this.currentPage < this.totalPages - 1) { this.currentPage++; this.updatePagination(); } }
    onPageSizeChange(): void { this.currentPage = 0; this.updatePagination(); }

    viewConversion(c: ConversionResponseDto): void {
        this.selectedConversion = c;
        this.showDetailModal = true;
    }

    closeModal(): void {
        this.showDetailModal = false;
        this.selectedConversion = null;
    }

    deleteConversion(c: ConversionResponseDto): void {
        this.conversionToDelete = c;
        this.showDeleteModal = true;
    }

    confirmDelete(): void {
        if (!this.conversionToDelete) return;
        this.adminService.deleteConversion(this.conversionToDelete.id).subscribe({
            next: () => {
                this.conversions = this.conversions.filter(c => c.id !== this.conversionToDelete!.id);
                this.applyFilters();
                this.cancelDelete();
            }
        });
    }

    cancelDelete(): void {
        this.conversionToDelete = null;
        this.showDeleteModal = false;
    }

    exportCsv(): void {
        const headers = ['ID', 'User Email', 'Input Format', 'Output Format', 'Prompt', 'Date'];
        const rows = this.filteredConversions.map(c => [
            c.id,
            c.userEmail ?? '',
            c.inputFormat,
            c.outputFormat,
            `"${(c.prompt ?? '').replace(/"/g, '""')}"`,
            new Date(c.createdAt).toISOString()
        ]);
        const csv = [headers, ...rows].map(r => r.join(',')).join('\n');
        const blob = new Blob([csv], { type: 'text/csv' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `conversions_${Date.now()}.csv`;
        a.click();
        URL.revokeObjectURL(url);
    }

    formatDate(date: Date | string): string {
        const d = typeof date === 'string' ? new Date(date) : date;
        return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric', hour: '2-digit', minute: '2-digit' });
    }
}
