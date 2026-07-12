import {
	bookSchema,
	freeApiEnvelopeSchema,
	paginatedDataSchema,
} from '@lib/api/schemas/freeapi.schemas';
import test from '@lib/FreeApiBaseTest';
import { expect } from '@playwright/test';

test.describe('Books', () => {
	test('should get paginated books list with valid schema', async ({
		freeApi,
	}) => {
		const response = await freeApi.getBooks({ limit: 2 });

		response.expectStatus(200);
		const body = await response.json();

		const schema = freeApiEnvelopeSchema(paginatedDataSchema(bookSchema));
		schema.parse(body);

		expect(body.data.data.length).toBeLessThanOrEqual(2);
	});

	test('should get a book by ID with deeply nested fields', async ({
		freeApi,
	}) => {
		const response = await freeApi.getBookById(1);

		response.expectStatus(200);
		const body = await response.json();

		const schema = freeApiEnvelopeSchema(bookSchema);
		schema.parse(body);

		const { volumeInfo } = body.data;
		expect(volumeInfo.title).toBeTruthy();
		expect(volumeInfo.authors.length).toBeGreaterThan(0);
		if (volumeInfo.pageCount !== undefined) {
			expect(volumeInfo.pageCount).toBeGreaterThanOrEqual(0);
		}
		expect(volumeInfo.categories).toBeInstanceOf(Array);
	});

	test('should get a random book with required nested structure', async ({
		freeApi,
	}) => {
		const response = await freeApi.getRandomBook();

		response.expectStatus(200);
		const body = await response.json();

		const schema = freeApiEnvelopeSchema(bookSchema);
		schema.parse(body);

		expect(body.data.kind).toBe('books#volume');
		expect(body.data.selfLink).toContain('googleapis.com');
		expect(body.data.volumeInfo.language).toBeTruthy();
	});
});
