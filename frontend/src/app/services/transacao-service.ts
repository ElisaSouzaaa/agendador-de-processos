import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Transacao } from '../model/transacao';

@Injectable({
  providedIn: 'root',
})
export class TransacaoService {
  private apiUrl = 'http://localhost:8080/arquivos';

  constructor(private http: HttpClient) {}

  getTransacoesPorArquivoId(arquivoId: number): Observable<Transacao[]> {
    return this.http.get<Transacao[]>(`${this.apiUrl}/${arquivoId}/transacoes`);
  }
}
