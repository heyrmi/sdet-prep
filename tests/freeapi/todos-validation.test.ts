import { freeApiErrorSchema } from '@lib/api/schemas/freeapi.schemas';
import test from '@lib/FreeApiBaseTest';
import { expect } from '@playwright/test';
import type { FreeApiError } from '@/types/freeapi';

test.describe('Todos Validation', () => {
	test('should reject creating a todo with empty body', async ({
		authedFreeApi,
	}) => {
		const response = await authedFreeApi.post<FreeApiError>('todos', {
			data: {},
			failOnStatusCode: false,
		});

		response.expectStatus(422);
		const body = await response.json();

		freeApiErrorSchema.parse(body);
		expect(body.errors.length).toBeGreaterThan(0);
	});

	test('should reject getting a todo with invalid ID format', async ({
		authedFreeApi,
	}) => {
		const response = await authedFreeApi.getTodoById('invalid-id');

		response.expectStatusIn([400, 404, 422, 500]);
		const body = await response.json();
		expect(body.success).toBe(false);
	});

	test('should reject updating a non-existent todo', async ({
		authedFreeApi,
	}) => {
		const response = await authedFreeApi.updateTodo(
			'000000000000000000000000',
			{ title: 'Ghost' },
		);

		response.expectStatusIn([404, 400]);
		const body = await response.json();
		expect(body.success).toBe(false);
	});

	test('should reject deleting a non-existent todo', async ({
		authedFreeApi,
	}) => {
		const response = await authedFreeApi.deleteTodo('000000000000000000000000');

		response.expectStatusIn([404, 400]);
		const body = await response.json();
		expect(body.success).toBe(false);
	});

	test('should reject toggling a non-existent todo', async ({
		authedFreeApi,
	}) => {
		const response = await authedFreeApi.toggleTodoStatus(
			'000000000000000000000000',
		);

		response.expectStatusIn([404, 400]);
		const body = await response.json();
		expect(body.success).toBe(false);
	});
});
