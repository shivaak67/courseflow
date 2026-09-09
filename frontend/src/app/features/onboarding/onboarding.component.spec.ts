import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { Subject, of, throwError } from 'rxjs';
import { ApiService } from '../../core/api/api.service';
import { AuthService } from '../../core/auth/auth.service';
import { TaskDto } from '../../core/api/api.models';
import { OnboardingComponent } from './onboarding.component';
import { OnboardingService } from './onboarding.service';
describe('Guided first session', () => {
  let component: OnboardingComponent;
  let guide: OnboardingService;
  let api: jasmine.SpyObj<ApiService>;
  const user = signal({ id: 'onboarding-test-a' });
  beforeEach(() => {
    localStorage.removeItem('prioritize.getting-started.v1.onboarding-test-a');
    localStorage.removeItem('prioritize.getting-started.v1.onboarding-test-b');
    user.set({ id: 'onboarding-test-a' });
    api = jasmine.createSpyObj('ApiService', [
      'createTask',
      'createCalendarEvent',
    ]);
    TestBed.configureTestingModule({
      providers: [
        { provide: ApiService, useValue: api },
        { provide: AuthService, useValue: { currentUser: user } },
      ],
    });
    guide = TestBed.inject(OnboardingService);
    component = TestBed.runInInjectionContext(() => new OnboardingComponent());
  });
  afterEach(() => {
    localStorage.removeItem('prioritize.getting-started.v1.onboarding-test-a');
    localStorage.removeItem('prioritize.getting-started.v1.onboarding-test-b');
  });
  it('prevents duplicate task creation and preserves a saved task if scheduling fails', () => {
    const response = new Subject<TaskDto>();
    api.createTask.and.returnValue(response);
    component.title = 'Essay';
    component.createTask();
    component.createTask();
    expect(api.createTask).toHaveBeenCalledTimes(1);
    response.next({ id: 't', title: 'Essay' } as TaskDto);
    response.complete();
    expect(component.step()).toBe(2);
    api.createCalendarEvent.and.returnValue(
      throwError(() => new Error('offline')),
    );
    component.schedule();
    expect(guide.state().taskId).toBe('t');
    expect(component.step()).toBe(2);
    expect(component.busy()).toBeFalse();
  });
  it('completes only after starting focus on the guided task', () => {
    guide.update({ taskId: 't', eventId: 'e', active: true });
    guide.startedFocus('another');
    expect(guide.state().completed).not.toBeTrue();
    guide.startedFocus('t');
    expect(guide.state().completed).toBeTrue();
  });
  it('keeps dismissal and progress separate for each account', () => {
    guide.dismiss();
    user.set({ id: 'onboarding-test-b' });
    expect(guide.state().dismissed).not.toBeTrue();
    user.set({ id: 'onboarding-test-a' });
    expect(guide.state().dismissed).toBeTrue();
  });
});
