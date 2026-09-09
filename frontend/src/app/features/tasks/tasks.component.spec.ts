import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { of } from 'rxjs';
import { ApiService } from '../../core/api/api.service';
import { TaskDto } from '../../core/api/api.models';
import { TasksComponent } from './tasks.component';

describe('Task discovery', () => {
  let component: TasksComponent;
  let api: jasmine.SpyObj<ApiService>;
  const task = (id: string, title: string, status = 'TODO', dueDate: string | null = null, priority = 'LOW') =>
    ({ id, title, status, dueDate, priority, description: null, dueTime: null } as TaskDto);
  beforeEach(() => {
    api = jasmine.createSpyObj('ApiService', ['listTasks', 'listCalendarEvents', 'createTask']);
    api.listTasks.and.returnValue(of([]));
    api.listCalendarEvents.and.returnValue(of([]));
    TestBed.configureTestingModule({ providers: [
      { provide: ApiService, useValue: api },
      { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: convertToParamMap({ view: 'overdue' }) } } },
    ] });
    component = TestBed.runInInjectionContext(() => new TasksComponent());
  });
  it('defaults to open work and sorts dated tasks before undated tasks', () => {
    component.tasks.set([task('1', 'Later'), task('2', 'Done', 'COMPLETED'), task('3', 'Due', 'TODO', '2026-09-05')]);
    expect(component.visibleTasks().map(t => t.id)).toEqual(['3', '1']);
  });
  it('combines case-insensitive search with status and priority sorting', () => {
    component.tasks.set([task('1', 'Read notes'), task('2', 'READ book', 'TODO', null, 'URGENT'), task('3', 'Read old', 'COMPLETED')]);
    component.search.set(' READ ');
    component.sort.set('priority');
    expect(component.visibleTasks().map(t => t.id)).toEqual(['2', '1']);
    component.view.set('completed');
    expect(component.visibleTasks().map(t => t.id)).toEqual(['3']);
  });
  it('honors dashboard links and excludes completed work from overdue', () => {
    component.ngOnInit();
    component.tasks.set([task('1', 'Late', 'TODO', '2000-01-01'), task('2', 'Done', 'COMPLETED', '2000-01-01')]);
    expect(component.visibleTasks().map(t => t.id)).toEqual(['1']);
    component.clearFilters();
    expect(component.visibleTasks().length).toBe(2);
  });
  it('rejects whitespace-only titles without making a request', () => {
    component.form.patchValue({ title: '   ' });
    component.submit();
    expect(api.createTask).not.toHaveBeenCalled();
    expect(component.form.controls.title.touched).toBeTrue();
    expect(component.form.controls.title.invalid).toBeTrue();
  });
});
