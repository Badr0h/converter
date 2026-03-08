import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AdminContactService, ContactMessageResponseDto } from '../../../core/services/api/admin/contact/admin-contact.service';

@Component({
    selector: 'app-admin-messages',
    standalone: true,
    imports: [CommonModule, RouterModule, FormsModule],
    templateUrl: './admin-messages.component.html',
    styleUrl: './admin-messages.component.scss'
})
export class AdminMessagesComponent implements OnInit {
    messages: ContactMessageResponseDto[] = [];
    selectedMessage: ContactMessageResponseDto | null = null;
    replyMessage: string = '';
    isLoading = false;
    isSending = false;
    currentPage = 0;
    totalPages = 0;

    constructor(private adminContactService: AdminContactService) { }

    ngOnInit(): void {
        this.loadMessages();
    }

    loadMessages(page: number = 0): void {
        this.isLoading = true;
        this.adminContactService.getAllMessages(page).subscribe({
            next: (response) => {
                this.messages = response.content;
                this.totalPages = response.totalPages;
                this.currentPage = response.number;
                this.isLoading = false;
            },
            error: (error) => {
                console.error('Error loading messages:', error);
                this.isLoading = false;
            }
        });
    }

    selectMessage(message: ContactMessageResponseDto): void {
        this.selectedMessage = message;
        this.replyMessage = '';

        if (!message.isRead) {
            this.adminContactService.getMessageById(message.id).subscribe({
                next: (updatedMessage) => {
                    const index = this.messages.findIndex(m => m.id === updatedMessage.id);
                    if (index !== -1) {
                        this.messages[index] = updatedMessage;
                    }
                    this.selectedMessage = updatedMessage;
                }
            });
        }
    }

    sendReply(): void {
        if (!this.selectedMessage || !this.replyMessage.trim()) return;

        this.isSending = true;
        this.adminContactService.replyToMessage(this.selectedMessage.id, { replyMessage: this.replyMessage }).subscribe({
            next: (updatedMessage) => {
                const index = this.messages.findIndex(m => m.id === updatedMessage.id);
                if (index !== -1) {
                    this.messages[index] = updatedMessage;
                }
                this.selectedMessage = updatedMessage;
                this.isSending = false;
                this.replyMessage = '';
            },
            error: (error) => {
                console.error('Error sending reply:', error);
                this.isSending = false;
            }
        });
    }

    getStatusBadge(message: ContactMessageResponseDto): string {
        if (message.isReplied) return 'badge-success';
        if (!message.isRead) return 'badge-warning';
        return 'badge-primary';
    }

    getStatusText(message: ContactMessageResponseDto): string {
        if (message.isReplied) return 'Replied';
        if (!message.isRead) return 'New';
        return 'Read';
    }
}
