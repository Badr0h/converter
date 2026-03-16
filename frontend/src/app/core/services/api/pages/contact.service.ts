import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';

export interface ContactMessageCreateDto {
    name: string;
    email: string;
    subject: string;
    message: string;
}

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

@Injectable({
    providedIn: 'root'
})
export class ContactService {
    private apiUrl = `${environment.apiUrl}/api/contact`;

    constructor(private http: HttpClient) { }

    submitMessage(dto: ContactMessageCreateDto): Observable<ContactMessageResponseDto> {
        return this.http.post<ContactMessageResponseDto>(this.apiUrl, dto);
    }
}
