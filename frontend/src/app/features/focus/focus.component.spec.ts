import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { Subject, of, throwError } from 'rxjs';
import { ApiService } from '../../core/api/api.service';
import { TaskDto, TimeEntryDto } from '../../core/api/api.models';
import { FocusComponent } from './focus.component';

describe('Focus session safeguards', () => {
  let component: FocusComponent;
  let api: jasmine.SpyObj<ApiService>;
  beforeEach(() => {
    api = jasmine.createSpyObj('ApiService', ['createTimeEntry']);
    TestBed.configureTestingModule({ providers: [{ provide: ApiService, useValue: api }] });
    component = TestBed.runInInjectionContext(() => new FocusComponent());
    component.tasks.set([{ id: 'task', title: 'Study', status: 'TODO' } as TaskDto]);
    component.selectedTaskId.set('task');
  });
  afterEach(() => component.ngOnDestroy());
  it('preserves elapsed time and excludes retry waiting after a failed save', fakeAsync(() => {
    api.createTimeEntry.and.returnValue(throwError(() => new Error('offline')));
    component.startSession();
    tick(120000);
    component.stopAndSave();
    expect(component.phase()).toBe('paused');
    expect(component.secondsRemaining()).toBe(48 * 60);
    const original = api.createTimeEntry.calls.mostRecent().args[0];
    tick(180000);
    api.createTimeEntry.and.returnValue(of({ id: 'entry' } as TimeEntryDto));
    component.stopAndSave();
    expect(api.createTimeEntry.calls.mostRecent().args[0]).toEqual(original);
    expect(component.phase()).toBe('complete');
  }));
  it('prevents duplicate submissions and navigation during a save', fakeAsync(() => {
    const response = new Subject<TimeEntryDto>();
    api.createTimeEntry.and.returnValue(response);
    component.startSession();
    tick(60000);
    component.stopAndSave();
    component.stopAndSave();
    expect(api.createTimeEntry).toHaveBeenCalledTimes(1);
    expect(component.canDeactivate()).toBeFalse();
    response.next({ id: 'entry' } as TimeEntryDto);
    expect(component.canDeactivate()).toBeTrue();
  }));
  it('asks before abandoning an active session and lets the user stay', fakeAsync(() => {
    const confirm = spyOn(window, 'confirm').and.returnValue(false);
    component.startSession();
    expect(component.canDeactivate()).toBeFalse();
    expect(confirm).toHaveBeenCalled();
    component.ngOnDestroy();
  }));
  it('warns before a browser refresh only while there is unsaved work', fakeAsync(() => {
    const event = { preventDefault: jasmine.createSpy(), returnValue: undefined } as unknown as BeforeUnloadEvent;
    component.warnBeforeUnload(event);
    expect(event.preventDefault).not.toHaveBeenCalled();
    component.startSession();
    component.warnBeforeUnload(event);
    expect(event.preventDefault).toHaveBeenCalled();
    component.ngOnDestroy();
  }));
});
