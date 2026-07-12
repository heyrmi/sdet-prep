import {
	freeApiEnvelopeSchema,
	freeApiErrorSchema,
	paginatedDataSchema,
	randomUserSchema,
} from '@lib/api/schemas/freeapi.schemas';
import test from '@lib/FreeApiBaseTest';
import { expect } from '@playwright/test';

test.describe('Random Users', () => {
	test('should get paginated users list with valid schema', async ({
		freeApi,
	}) => {
		const response = await freeApi.getUsers({ limit: 2 });

		response.expectStatus(200);
		const body = await response.json();

		const schema = freeApiEnvelopeSchema(paginatedDataSchema(randomUserSchema));
		schema.parse(body);

		expect(body.data.data.length).toBeLessThanOrEqual(2);
		expect(body.data.currentPageItems).toBeLessThanOrEqual(2);
	});

	test('should get a single user by ID', async ({ freeApi }) => {
		const response = await freeApi.getUserById(1);

		response.expectStatus(200);
		const body = await response.json();

		const schema = freeApiEnvelopeSchema(randomUserSchema);
		schema.parse(body);

		expect(body.data.id).toBe(1);
	});

	test('should get a random user with valid schema', async ({ freeApi }) => {
		const response = await freeApi.getRandomUser();

		response.expectStatus(200);
		const body = await response.json();

		const schema = freeApiEnvelopeSchema(randomUserSchema);
		schema.parse(body);

		expect(body.data.id).toEqual(expect.any(Number));
		expect(body.data.email).toContain('@');
	});

	test('should support pagination - page 2', async ({ freeApi }) => {
		const response = await freeApi.getUsers({ page: 2, limit: 3 });

		response.expectStatus(200);
		const body = await response.json();

		expect(body.data.page).toBe(2);
		expect(body.data.limit).toBe(3);
		expect(body.data.previousPage).toBe(true);
		expect(body.data.currentPageItems).toBeLessThanOrEqual(3);
	});

	test('should support search query', async ({ freeApi }) => {
		const response = await freeApi.getUsers({ query: 'joseph', limit: 5 });

		response.expectStatus(200);
		const body = await response.json();

		expect(body.data.totalItems).toBeGreaterThan(0);
	});

	test('should return 404 for non-existent user', async ({ freeApi }) => {
		const response = await freeApi.getUserById(99999);

		response.expectStatus(404);
		const body = await response.json();

		const errorBody = freeApiErrorSchema.parse(body);
		expect(errorBody.success).toBe(false);
		expect(errorBody.data).toBeNull();
	});
});
