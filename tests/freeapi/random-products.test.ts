import {
	freeApiEnvelopeSchema,
	freeApiErrorSchema,
	paginatedDataSchema,
	productSchema,
} from '@lib/api/schemas/freeapi.schemas';
import test from '@lib/FreeApiBaseTest';
import { expect } from '@playwright/test';

test.describe('Random Products', () => {
	test('should get paginated products list with valid schema', async ({
		freeApi,
	}) => {
		const response = await freeApi.getProducts({ limit: 3 });

		response.expectStatus(200);
		const body = await response.json();

		const schema = freeApiEnvelopeSchema(paginatedDataSchema(productSchema));
		schema.parse(body);

		expect(body.data.data.length).toBeLessThanOrEqual(3);
	});

	test('should get a product by ID with valid fields', async ({ freeApi }) => {
		const response = await freeApi.getProductById(1);

		response.expectStatus(200);
		const body = await response.json();

		const schema = freeApiEnvelopeSchema(productSchema);
		schema.parse(body);

		expect(body.data.id).toBe(1);
		expect(body.data.images.length).toBeGreaterThan(0);
	});

	test('should get a random product with valid numeric ranges', async ({
		freeApi,
	}) => {
		const response = await freeApi.getRandomProduct();

		response.expectStatus(200);
		const body = await response.json();

		const schema = freeApiEnvelopeSchema(productSchema);
		schema.parse(body);

		expect(body.data.price).toBeGreaterThan(0);
		expect(body.data.rating).toBeGreaterThanOrEqual(0);
		expect(body.data.rating).toBeLessThanOrEqual(5);
		expect(body.data.stock).toBeGreaterThanOrEqual(0);
	});

	test('should return 404 for non-existent product', async ({ freeApi }) => {
		const response = await freeApi.getProductById(99999);

		response.expectStatus(404);
		const body = await response.json();

		const errorBody = freeApiErrorSchema.parse(body);
		expect(errorBody.success).toBe(false);
	});
});
