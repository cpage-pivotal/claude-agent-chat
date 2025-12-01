import { Component, signal, effect, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatCardModule } from '@angular/material/card';
import { ChatService, ChatMessage } from '../../services/chat.service';

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [
    CommonModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatCardModule
  ],
  templateUrl: './chat.component.html',
  styleUrl: './chat.component.scss'
})
export class ChatComponent {
  @ViewChild('messagesContainer') private messagesContainer!: ElementRef;
  
  protected messages = signal<ChatMessage[]>([]);
  protected userInput = signal('');
  protected isStreaming = signal(false);
  protected claudeAvailable = signal(true);
  protected claudeVersion = signal('');
  protected claudeMessage = signal('');

  constructor(private chatService: ChatService) {
    // Check Claude Code availability on init
    this.chatService.checkHealth().then(health => {
      this.claudeAvailable.set(health.available);
      this.claudeVersion.set(health.version);
      this.claudeMessage.set(health.message || '');
    });

    // Auto-scroll when messages update with smooth animation
    effect(() => {
      this.messages();
      // Use requestAnimationFrame for smoother scroll animation
      requestAnimationFrame(() => {
        setTimeout(() => this.scrollToBottom(), 50);
      });
    });
  }

  protected sendMessage(): void {
    const prompt = this.userInput().trim();
    if (!prompt || this.isStreaming()) {
      return;
    }

    // Add user message
    const userMessage: ChatMessage = {
      role: 'user',
      content: prompt,
      timestamp: new Date()
    };
    this.messages.update(msgs => [...msgs, userMessage]);
    this.userInput.set('');
    this.isStreaming.set(true);

    // Add assistant message placeholder
    const assistantMessage: ChatMessage = {
      role: 'assistant',
      content: '',
      timestamp: new Date(),
      streaming: true
    };
    this.messages.update(msgs => [...msgs, assistantMessage]);

    // Stream the response
    this.chatService.sendMessage(prompt).subscribe({
      next: (chunk: string) => {
        this.messages.update(msgs => {
          const lastMsg = msgs[msgs.length - 1];
          if (lastMsg.role === 'assistant') {
            return [
              ...msgs.slice(0, -1),
              {
                ...lastMsg,
                content: lastMsg.content + chunk
              }
            ];
          }
          return msgs;
        });
      },
      error: (error) => {
        console.error('Chat error:', error);
        this.messages.update(msgs => {
          const lastMsg = msgs[msgs.length - 1];
          if (lastMsg.role === 'assistant' && lastMsg.streaming) {
            return [
              ...msgs.slice(0, -1),
              {
                ...lastMsg,
                content: lastMsg.content || 'Error: Failed to get response from Claude Code.',
                streaming: false
              }
            ];
          }
          return msgs;
        });
        this.isStreaming.set(false);
      },
      complete: () => {
        this.messages.update(msgs => {
          const lastMsg = msgs[msgs.length - 1];
          if (lastMsg.role === 'assistant') {
            return [
              ...msgs.slice(0, -1),
              {
                ...lastMsg,
                streaming: false
              }
            ];
          }
          return msgs;
        });
        this.isStreaming.set(false);
      }
    });
  }

  protected onKeyPress(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  protected onInputChange(event: Event): void {
    const target = event.target as HTMLTextAreaElement;
    this.userInput.set(target.value);
  }

  private scrollToBottom(): void {
    if (this.messagesContainer) {
      const element = this.messagesContainer.nativeElement;
      // Smooth scroll to bottom
      element.scrollTo({
        top: element.scrollHeight,
        behavior: 'smooth'
      });
    }
  }
}

