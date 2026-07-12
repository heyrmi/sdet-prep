import { todoSchema } from '@lib/api/schemas/freeapi.schemas';
import test from '@lib/FreeApiBaseTest';
import { expect } from '@playwright/test';

test.describe('Todos CRUD Lifecycle', () => {
	test.describe.configure({ mode: 'serial' });

	let todoId: string;

	test('should create a new todo', async ({ authedFreeApi }) => {
		const response = await authedFreeApi.createTodo(
			'Playwright Todo',
			'Created by Playwright test',
		);

		response.expectStatus(201);
		const body = await response.json();

		expect(body.success).toBe(true);
		todoSchema.parse(body.data);
		expect(body.data.title).toBe('Playwright Todo');
		expect(body.data.description).toBe('Created by Playwright test');
		expect(body.data.isComplete).toBe(false);

		todoId = body.data._id;
	});

	test('should read the created todo by ID', async ({ authedFreeApi }) => {
		const response = await authedFreeApi.getTodoById(todoId);

		response.expectStatus(200);
		const body = await response.json();

		expect(body.success).toBe(true);
		todoSchema.parse(body.data);
		expect(body.data._id).toBe(todoId);
		expect(body.data.title).toBe('Playwright Todo');
	});

	test('should list todos containing the created one', async ({
		authedFreeApi,
	}) => {
		const response = await authedFreeApi.getTodos();

		response.expectStatus(200);
		const body = await response.json();

		expect(body.success).toBe(true);
		expect(Array.isArray(body.data)).toBe(true);

		const found = body.data.find((t: { _id: string }) => t._id === todoId);
		expect(found).toBeTruthy();
	});

	test('should update the todo title and description', async ({
		authedFreeApi,
	}) => {
		const response = await authedFreeApi.updateTodo(todoId, {
			title: 'Updated Playwright Todo',
			description: 'Updated by test',
		});

		response.expectStatus(200);
		const body = await response.json();

		expect(body.success).toBe(true);
		todoSchema.parse(body.data);
		expect(body.data.title).toBe('Updated Playwright Todo');
		expect(body.data.description).toBe('Updated by test');
	});

	test('should toggle todo status to complete', async ({ authedFreeApi }) => {
		const response = await authedFreeApi.toggleTodoStatus(todoId);

		response.expectStatus(200);
		const body = await response.json();

		expect(body.success).toBe(true);
		expect(body.data.isComplete).toBe(true);
	});

	test('should toggle todo status back to incomplete', async ({
		authedFreeApi,
	}) => {
		const response = await authedFreeApi.toggleTodoStatus(todoId);

		response.expectStatus(200);
		const body = await response.json();

		expect(body.success).toBe(true);
		expect(body.data.isComplete).toBe(false);
	});

	test('should delete the todo', async ({ authedFreeApi }) => {
		const response = await authedFreeApi.deleteTodo(todoId);

		response.expectStatus(200);
		const body = await response.json();

		expect(body.success).toBe(true);
		expect(body.data.deletedTodo._id).toBe(todoId);
	});

	test('should return 404 for deleted todo', async ({ authedFreeApi }) => {
		const response = await authedFreeApi.getTodoById(todoId);

		response.expectStatusIn([404, 400]);
		const body = await response.json();
		expect(body.success).toBe(false);
	});
});
