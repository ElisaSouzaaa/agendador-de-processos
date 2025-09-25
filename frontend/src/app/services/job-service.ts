import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Job } from '../model/job';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class JobService {
  private apiUrl = 'http://localhost:8080/jobs';

  constructor(private http: HttpClient) {}

  criarJob(job: Partial<Job>): Observable<Job> {
    return this.http.post<Job>(this.apiUrl, job);
  }

  listarJobs(): Observable<Job[]> {
    return this.http.get<Job[]>(this.apiUrl)
  }

  buscarJobId(id: number): Observable<Job>{
    return this.http.get<Job>(`${this.apiUrl}/${id}`);
  }

  atualizarJob(id: number, job: Partial<Job>): Observable<Job> {
    return this.http.put<Job>(`${this.apiUrl}/${id}`, job)
  }

  deletarJob(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`)
  }
}