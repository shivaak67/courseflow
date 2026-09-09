import { TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import { ApiService } from '../../core/api/api.service';
import { CanvasFeedStatus } from '../../core/api/api.models';
import { CanvasConnectionComponent } from './canvas-connection.component';
describe('Canvas calendar connection', () => {
  let component: CanvasConnectionComponent;
  let api: jasmine.SpyObj<ApiService>;
  const connected: CanvasFeedStatus = {
    connected: true,
    host: 'school.instructure.com',
    timezone: 'America/Chicago',
    lastSyncedAt: '2026-09-09T14:00:00Z',
    nextSyncAt: null,
    itemCount: 3,
    error: null,
  };
  beforeEach(() => {
    api = jasmine.createSpyObj('ApiService', [
      'getCanvasFeed',
      'connectCanvasFeed',
      'syncCanvasFeed',
      'disconnectCanvasFeed',
    ]);
    TestBed.configureTestingModule({
      providers: [{ provide: ApiService, useValue: api }],
    });
    component = TestBed.runInInjectionContext(
      () => new CanvasConnectionComponent(),
    );
  });
  it('blocks duplicate connects and clears the private link only on success', () => {
    const result = new Subject<CanvasFeedStatus>();
    api.connectCanvasFeed.and.returnValue(result);
    component.feedUrl =
      'https://school.instructure.com/feeds/calendars/user_fixture.ics';
    component.connect();
    component.connect();
    expect(api.connectCanvasFeed).toHaveBeenCalledTimes(1);
    expect(component.busy()).toBeTrue();
    result.next(connected);
    expect(component.feedUrl).toBe('');
    expect(component.status()?.itemCount).toBe(3);
    expect(component.busy()).toBeFalse();
  });
  it('keeps the current connection and replacement input when a refresh fails', () => {
    component.status.set(connected);
    component.feedUrl = 'replacement';
    api.connectCanvasFeed.and.returnValue(
      throwError(() => ({ error: { message: 'Feed unavailable' } })),
    );
    component.connect();
    expect(component.status()).toEqual(connected);
    expect(component.feedUrl).toBe('replacement');
    expect(component.error()).toBe('Feed unavailable');
    expect(component.busy()).toBeFalse();
  });
  it('does not claim a sync succeeded when the server retained an old snapshot', () => {
    api.syncCanvasFeed.and.returnValue(
      of({ ...connected, error: 'Previous items kept' }),
    );
    component.sync();
    expect(component.message()).toBe('');
    expect(component.status()?.error).toBe('Previous items kept');
  });
  it('requires the explicit disconnect step', () => {
    component.disconnect();
    expect(api.disconnectCanvasFeed).not.toHaveBeenCalled();
  });
});
