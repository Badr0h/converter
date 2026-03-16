import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';

export interface ContactMessageResponseDto {
    id: number;
    name: string;
    email: string;
    subject: string;
    message: string;
    replyMessage: string | null;
    repliedAt: string | null;
    isRead: boolean;
    isReplied: boolean;
    createdAt: string;
    userId: number;
}

export interface ContactMessageReplyDto {
    replyMessage: string;
}

export interface PageResponse<T> {
    content: T[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
}

@Injectable({
    providedIn: 'root'
})
export class AdminContactService {
    private apiUrl = `${environment.apiUrl}/api/admin/contact`;

    constructor(private http: HttpClient) { }

    getAllMessages(page: number = 0, size: number = 20): Observable<PageResponse<ContactMessageResponseDto>> {
        const params = new HttpParams()
            .set('page', page.toString())
            .set('size', size.toString());
        return this.http.get<PageResponse<ContactMessageResponseDto>>(this.apiUrl, { params });
    }

    getMessageById(id: number): Observable<ContactMessageResponseDto> {
        return this.http.get<ContactMessageResponseDto>(`${this.apiUrl}/${id}`);
    }

    replyToMessage(id: number, dto: ContactMessageReplyDto): Observable<ContactMessageResponseDto> {
        return this.http.post<ContactMessageResponseDto>(`${this.apiUrl}/${id}/reply`, dto);
    }
}
