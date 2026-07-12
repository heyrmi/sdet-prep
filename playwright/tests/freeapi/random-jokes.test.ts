import {
	freeApiEnvelopeSchema,
	freeApiErrorSchema,
	jokeSchema,
	paginatedDataSchema,
} from '@lib/api/schemas/freeapi.schemas';
import test from '@lib/FreeApiBaseTest';
import { expect } from '@playwright/test';

test.describe('Random Jokes', () => {
	test('should get paginated jokes list with valid schema', async ({
		freeApi,
	}) => {
		const response = await freeApi.getJokes({ limit: 2 });

		response.expectStatus(200);
		const body = await response.json();

		const schema = freeApiEnvelopeSchema(paginatedDataSchema(jokeSchema));
		schema.parse(body);

		expect(body.data.data.length).toBeLessThanOrEqual(2);
	});

	test('should get a joke by ID', async ({ freeApi }) => {
		const response = await freeApi.getJokeById(1);

		response.expectStatus(200);
		const body = await response.json();

		const schema = freeApiEnvelopeSchema(jokeSchema);
		schema.parse(body);

		expect(body.data.id).toBe(1);
		expect(body.data.content).toBeTruthy();
	});

	test('should get a random joke', async ({ freeApi }) => {
		const response = await freeApi.getRandomJoke();

		response.expectStatus(200);
		const body = await response.json();

		const schema = freeApiEnvelopeSchema(jokeSchema);
		schema.parse(body);

		expect(body.data.id).toEqual(expect.any(Number));
		expect(body.data.content.length).toBeGreaterThan(0);
	});

	test('should return 404 for non-existent joke', async ({ freeApi }) => {
		const response = await freeApi.getJokeById(999999);

		response.expectStatus(404);
		const body = await response.json();

		const errorBody = freeApiErrorSchema.parse(body);
		expect(errorBody.success).toBe(false);
	});
});
