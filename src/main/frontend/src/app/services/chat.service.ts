import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
  timestamp: Date;
  streaming?: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class ChatService {
  private readonly apiUrl = '/api/chat';

  sendMessage(prompt: string): Observable<string> {
    return new Observable(observer => {
      let reader: ReadableStreamDefaultReader<Uint8Array> | null = null;
      let buffer = '';

      // Create POST request to send the prompt
      fetch(`${this.apiUrl}/stream`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ prompt })
      })
      .then(response => {
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }
        return response.body;
      })
      .then(body => {
        if (!body) {
          throw new Error('Response body is empty');
        }
        
        reader = body.getReader();
        const decoder = new TextDecoder();

        const processStream = (): Promise<void> => {
          return reader!.read().then(({ done, value }) => {
            if (done) {
              observer.complete();
              return;
            }

            buffer += decoder.decode(value, { stream: true });
            const lines = buffer.split('\n');
            
            // Keep the last incomplete line in the buffer
            buffer = lines.pop() || '';
            
            let currentEvent = '';
            for (const line of lines) {
              if (line.startsWith('event:')) {
                currentEvent = line.substring(6).trim();
              } else if (line.startsWith('data:')) {
                const data = line.substring(5).trim();
                if (data) {
                  if (currentEvent === 'error') {
                    observer.error(new Error(data));
                    return;
                  }
                  observer.next(data);
                }
                currentEvent = ''; // Reset after processing
              }
            }

            return processStream();
          });
        };

        return processStream();
      })
      .catch(error => {
        console.error('Stream error:', error);
        observer.error(error);
      });

      return () => {
        // Cleanup on unsubscribe - cancel the stream reader
        if (reader) {
          reader.cancel().catch(err => console.error('Error canceling stream:', err));
        }
      };
    });
  }

  checkHealth(): Promise<{ available: boolean; version: string; message?: string }> {
    return fetch(`${this.apiUrl}/health`)
      .then(response => response.json())
      .catch(error => {
        console.error('Health check failed:', error);
        return { available: false, version: 'unknown', message: 'Health check endpoint not reachable' };
      });
  }
}

