import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ConversionResponseDto, ConversionCreateDto } from '../models/conversion.model';

@Injectable({
  providedIn: 'root'
})
export class ConversionService {
  private apiUrl = `${environment.apiUrl}/conversions`;

  constructor(private http: HttpClient) {}

  /**
   * Convert text/content from one format to another using AI
   */
  createConversion(request: ConversionCreateDto): Observable<ConversionResponseDto> {
    return this.http.post<ConversionResponseDto>(`${this.apiUrl}`, request);
  }

  /**
   * Get all conversions (history)
   
  getAllConversions(): Observable<ConversionResponseDto[]> {
    return this.http.get<ConversionResponseDto[]>(`${this.apiUrl}`);
  }
*/
  /**
   * Get conversion history for the current user
   */
  getConversionHistory(): Observable<ConversionResponseDto[]> {
    return this.http.get<ConversionResponseDto[]>(`${this.apiUrl}`);
  }

  /**
   * Get a specific conversion by ID
   */
  getConversionById(id: number): Observable<ConversionResponseDto> {
    return this.http.get<ConversionResponseDto>(`${this.apiUrl}/${id}`);
  }

  /**
   * Delete a conversion
   */
  deleteConversion(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}