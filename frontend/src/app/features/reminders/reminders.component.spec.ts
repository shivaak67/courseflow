import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ApiService } from '../../core/api/api.service';
import { ReminderDto, TaskDto } from '../../core/api/api.models';
import { RemindersComponent } from './reminders.component';

describe('Reminder selection and history', () => {
  let component: RemindersComponent;
  let api: jasmine.SpyObj<ApiService>;
  beforeEach(() => {
    api = jasmine.createSpyObj('ApiService', ['scheduleReminders', 'listReminders', 'clearReminderHistory']);
    api.listReminders.and.returnValue(of([]));
    TestBed.configureTestingModule({ providers: [{ provide: ApiService, useValue: api }] });
    component = TestBed.runInInjectionContext(() => new RemindersComponent());
    component.tasks.set(['one', 'two'].map(id => ({ id, title: id, status: 'TODO', dueDate: '2099-01-01' } as TaskDto)));
    component.scheduleForm.patchValue({ emailEnabled: true });
  });
  it('selects all, toggles individuals and clears selection on type changes', () => {
    component.selectAll(); expect(component.selectedIds()).toEqual(['one', 'two']);
    component.toggleItem('one'); expect(component.selectedIds()).toEqual(['two']);
    component.scheduleForm.patchValue({ entityKind: 'CALENDAR_EVENT' }); component.onEntityKindChange();
    expect(component.selectedIds()).toEqual([]);
  });
  it('schedules each selection and keeps failed items selected without retrying successful ones', () => {
    api.scheduleReminders.and.callFake(request => request.relatedEntityId === 'one'
      ? of({ reminders: [] }) : throwError(() => ({ error: { message: 'Too late' } })));
    component.selectAll(); component.scheduleReminders();
    expect(api.scheduleReminders.calls.count()).toBe(2);
    expect(component.selectedIds()).toEqual(['two']);
    expect(component.scheduleMessage()).toContain('1 of 2'); expect(component.scheduleMessage()).toContain('Too late');
    expect(component.saving()).toBeFalse();
  });
  it('requires selection and confirmation, preserves pending and processing activity', () => {
    component.scheduleReminders(); expect(api.scheduleReminders).not.toHaveBeenCalled();
    component.clearHistory(); expect(api.clearReminderHistory).not.toHaveBeenCalled();
    component.reminders.set(['PENDING', 'PROCESSING', 'SENT', 'FAILED', 'CANCELLED'].map(status => ({ status } as ReminderDto)));
    component.confirmClearHistory.set(true); api.clearReminderHistory.and.returnValue(of(undefined)); component.clearHistory();
    expect(component.reminders().map(r => r.status)).toEqual(['PENDING', 'PROCESSING']);
  });
  it('keeps history visible on failure', () => {
    component.reminders.set([{ status: 'SENT' } as ReminderDto]); component.confirmClearHistory.set(true);
    api.clearReminderHistory.and.returnValue(throwError(() => new Error('offline'))); component.clearHistory();
    expect(component.history().length).toBe(1); expect(component.clearingHistory()).toBeFalse();
  });
});
