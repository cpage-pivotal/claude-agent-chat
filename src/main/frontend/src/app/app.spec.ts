import { provideZonelessChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { App } from './app';
import { ChatService } from './services/chat.service';

describe('App', () => {
  const mockChatService = {
    checkHealth: () => Promise.resolve({ available: true, version: '1.0.0', message: '' }),
    createSession: () => Promise.resolve('mock-session-id'),
    sendMessage: () => ({
      subscribe: () => ({})
    })
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideZonelessChangeDetection(),
        { provide: ChatService, useValue: mockChatService }
      ]
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should render title', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.toolbar-title')?.textContent).toContain('Claude Agent Chat');
  });
});
