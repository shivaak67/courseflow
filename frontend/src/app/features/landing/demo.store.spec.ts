import { DemoStore } from './demo.store';
describe('Public demo isolation', () => {
  it('edits fictional records and resets them without a network service', () => {
    const store = new DemoStore();
    const first = store.tasks()[0];
    store.updateTask(first.id, { status: 'COMPLETED' });
    expect(store.tasks()[0].status).toBe('COMPLETED');
    store.addTask('A visitor example');
    expect(store.tasks().length).toBe(4);
    store.reset();
    expect(store.tasks().length).toBe(3);
    expect(store.tasks().every((t) => t.status === 'TODO')).toBeTrue();
  });
  it('does not share changes between separate visits', () => {
    const first = new DemoStore();
    const second = new DemoStore();
    first.addTask('Only in the first visit');
    expect(second.tasks().length).toBe(3);
  });
});
