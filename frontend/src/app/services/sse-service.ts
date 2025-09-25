import { Injectable, NgZone } from '@angular/core';
import { Observable } from 'rxjs';
import { Job } from '../model/job'; 

export interface DeleteEvent {
  action: 'delete';
  jobId: number;
}

@Injectable({
  providedIn: 'root',
})
export class SseService {
  private eventSource!: EventSource;

  constructor(private zone: NgZone) {}

  getServerSentEvents(url: string): Observable<Job | Job[] | DeleteEvent> {
    return new Observable(subscriber => {
      this.eventSource = new EventSource(url);

      this.eventSource.addEventListener('message', (event: MessageEvent) => {
        const eventData = JSON.parse(event.data);
        
        this.zone.run(() => {
          subscriber.next(eventData);
        });
      });

      this.eventSource.onerror = (error) => {
        this.zone.run(() => {
          subscriber.error(error);
          this.closeConnection();
        });
      };
      
      return () => this.closeConnection();
    });
  }
  
  private closeConnection(): void {
    if (this.eventSource) {
      this.eventSource.close();
    }
  }
}