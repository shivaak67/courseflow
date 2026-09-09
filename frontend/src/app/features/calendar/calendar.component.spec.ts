import { TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import { ApiService } from '../../core/api/api.service';
import { CalendarEventDto, TaskDto } from '../../core/api/api.models';
import { CalendarComponent, eventDayKeys } from './calendar.component';

describe('Interactive calendar', () => {
  let component: CalendarComponent;
  let api: jasmine.SpyObj<ApiService>;
  beforeEach(() => {
    api = jasmine.createSpyObj('ApiService', [
      'listTasks',
      'listCalendarEvents',
      'updateTask',
      'createCalendarEvent',
      'updateCalendarEvent',
    ]);
    api.listTasks.and.returnValue(of([]));
    api.listCalendarEvents.and.returnValue(of([]));
    TestBed.configureTestingModule({
      providers: [{ provide: ApiService, useValue: api }],
    });
    component = TestBed.runInInjectionContext(() => new CalendarComponent());
  });
  afterEach(() => component.ngOnDestroy());
  it('retains task details and status when only editing its deadline', () => {
    const task = {
      id: 't',
      title: 'Essay',
      description: 'Keep these notes',
      categoryId: 'c',
      projectId: 'p',
      priority: 'HIGH',
      status: 'COMPLETED',
      estimatedMinutes: 90,
    } as TaskDto;
    component.tasks.set([task]);
    component.entityId = 't';
    component.editor.set('task');
    component.title = 'Essay';
    component.dueDate = '2026-09-20';
    api.updateTask.and.returnValue(of(task));
    component.save();
    expect(api.updateTask).toHaveBeenCalledWith(
      't',
      jasmine.objectContaining({
        description: 'Keep these notes',
        categoryId: 'c',
        projectId: 'p',
        priority: 'HIGH',
        status: 'COMPLETED',
        estimatedMinutes: 90,
        dueDate: '2026-09-20',
      }),
    );
  });
  it('rejects inverted time ranges without a request', () => {
    component.editor.set('new');
    component.title = 'Study';
    component.startLocal = '2026-09-20T12:00';
    component.endLocal = '2026-09-20T11:00';
    component.save();
    expect(api.createCalendarEvent).not.toHaveBeenCalled();
    expect(component.formError()).toContain('end after');
  });
  it('retains the editor after a failed save and prevents duplicate requests', () => {
    component.editor.set('new');
    component.title = 'Study';
    component.startLocal = '2026-09-20T12:00';
    component.endLocal = '2026-09-20T13:00';
    const response = new Subject<CalendarEventDto>();
    api.createCalendarEvent.and.returnValue(response);
    component.save();
    component.save();
    expect(api.createCalendarEvent).toHaveBeenCalledTimes(1);
    response.error(new Error('offline'));
    expect(component.editor()).toBe('new');
    expect(component.title).toBe('Study');
    expect(component.saving()).toBeFalse();
  });
  it('ignores stale loads after navigating to another month', () => {
    const oldTasks = new Subject<TaskDto[]>();
    api.listTasks.and.returnValue(oldTasks);
    component.reload();
    api.listTasks.and.returnValue(of([]));
    component.nextMonth();
    oldTasks.next([{ id: 'old' } as TaskDto]);
    oldTasks.complete();
    expect(component.tasks()).toEqual([]);
  });
  it('shows overnight events on both days and treats midnight ends as exclusive', () => {
    const event = {
      startAt: new Date(2026, 8, 9, 23).toISOString(),
      endAt: new Date(2026, 8, 10, 1).toISOString(),
    } as CalendarEventDto;
    expect(eventDayKeys(event)).toEqual(['2026-09-09', '2026-09-10']);
    event.endAt = new Date(2026, 8, 10, 0).toISOString();
    expect(eventDayKeys(event)).toEqual(['2026-09-09']);
  });
});
