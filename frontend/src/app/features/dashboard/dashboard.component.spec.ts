import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { ApiService } from '../../core/api/api.service';
import { AuthService } from '../../core/auth/auth.service';
import { TaskDto, CalendarEventDto } from '../../core/api/api.models';
import { DashboardComponent } from './dashboard.component';

describe('Dashboard deadlines', () => {
  let component: DashboardComponent;
  const task = (id: string, dueDate: string, status = 'TODO', dueTime: string | null = null) =>
    ({ id, title: id, dueDate, status, dueTime } as TaskDto);
  const event = (id: string, startAt: string, endAt: string) =>
    ({ id, title: id, startAt, endAt, allDay: false } as CalendarEventDto);
  beforeEach(() => {
    jasmine.clock().install();
    jasmine.clock().mockDate(new Date(2026, 8, 5, 10));
    TestBed.configureTestingModule({ providers: [
      { provide: ApiService, useValue: {} },
      { provide: AuthService, useValue: { currentUser: signal(null) } },
    ] });
    component = TestBed.runInInjectionContext(() => new DashboardComponent());
  });
  afterEach(() => jasmine.clock().uninstall());
  it('shows older overdue work and next-week tasks while excluding completed work', () => {
    component.tasks.set([task('old', '2026-08-01'), task('next', '2026-09-08'), task('done', '2026-09-02', 'COMPLETED'), task('far', '2026-09-20')]);
    expect(component.upcomingDeadlines().map(t => t.id)).toEqual(['task-old', 'task-next']);
    expect(component.upcomingDeadlines()[0].when).toContain('Overdue');
  });
  it('excludes ended events but retains ongoing events and sorts timestamps consistently', () => {
    component.events.set([
      event('ended', '2026-09-03T10:00:00', '2026-09-03T11:00:00'),
      event('ongoing', '2026-09-04T10:00:00', '2026-09-06T11:00:00'),
      event('future', '2026-09-08T10:00:00', '2026-09-08T11:00:00'),
    ]);
    expect(component.upcomingDeadlines().map(t => t.id)).toEqual(['event-ongoing', 'event-future']);
  });
  it('includes tasks due today without a specific time', () => {
    component.tasks.set([task('anytime', '2026-09-05'), task('timed', '2026-09-05', 'TODO', '09:00')]);
    expect(component.todayPlan().map(t => t.id)).toEqual(['task-timed', 'task-anytime']);
    expect(component.todayPlan()[1].time).toBe('Any time');
  });
  it('counts a passed due time today as overdue consistently with the deadline list', () => {
    component.tasks.set([task('late', '2026-09-05', 'TODO', '09:00'), task('later', '2026-09-05', 'TODO', '11:00')]);
    expect(component.stats().find(stat => stat.label === 'Overdue')?.value).toBe(1);
    expect(component.upcomingDeadlines().filter(item => item.overdue).length).toBe(1);
  });
  it('does not count cancelled tasks against weekly completion', () => {
    component.tasks.set([task('cancelled', '2026-09-02', 'CANCELLED'), task('done', '2026-09-02', 'COMPLETED')]);
    expect(component.weeklyProgress()).toEqual({ done: 1, total: 1, percent: 100 });
  });
});
