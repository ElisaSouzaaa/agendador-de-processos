import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ArquivoRetorno } from '../model/arquivo-retorno';
import { Job } from '../model/job';

@Injectable({
  providedIn: 'root',
})
export class ArquivoRetornoService {
  private apiUrl = 'http://localhost:8080/arquivos';

  constructor(private http: HttpClient) {}

  getArquivosPorJobId(jobId: number): Observable<ArquivoRetorno[]> {
    return this.http.get<ArquivoRetorno[]>(`${this.apiUrl}/job/${jobId}`);
  }

  uploadoArquivo(jobId: number, arquivo: File): Observable<string> {
    const formData = new FormData();
    formData.append('file', arquivo);

    return this.http.post(`${this.apiUrl}/upload/${jobId}`, formData,{
      responseType: 'text'
    })
  }

  listarArquivos(): Observable<ArquivoRetorno[]> {
    return this.http.get<ArquivoRetorno[]>(this.apiUrl);
  }

  buscarArquivoId(id: number): Observable<ArquivoRetorno> {
    return this.http.get<ArquivoRetorno>(`${this.apiUrl}/${id}`);
  }

  listarArquivoJobId(jobId: number): Observable<ArquivoRetorno[]> {
    return this.http.get<ArquivoRetorno[]>(`${this.apiUrl}/job/${jobId}`);
  }

  deletarArquivoId(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

    deleteArquivo(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
