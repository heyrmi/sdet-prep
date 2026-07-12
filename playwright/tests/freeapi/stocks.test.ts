import {
	freeApiEnvelopeSchema,
	paginatedDataSchema,
	stockSchema,
} from '@lib/api/schemas/freeapi.schemas';
import test from '@lib/FreeApiBaseTest';
import { expect } from '@playwright/test';

test.describe('Stocks', () => {
	test('should get paginated stocks list with valid schema', async ({
		freeApi,
	}) => {
		const response = await freeApi.getStocks({ limit: 3 });

		response.expectStatus(200);
		const body = await response.json();

		const schema = freeApiEnvelopeSchema(paginatedDataSchema(stockSchema));
		schema.parse(body);

		expect(body.data.data.length).toBeLessThanOrEqual(3);
	});

	test('should have valid stock symbol format', async ({ freeApi }) => {
		const response = await freeApi.getStocks({ limit: 5 });

		response.expectStatus(200);
		const body = await response.json();

		for (const stock of body.data.data) {
			expect(stock.Symbol).toMatch(/^[A-Z0-9&]+$/);
			expect(stock.Name).toBeTruthy();
			expect(stock.ISIN).toMatch(/^INE/);
		}
	});

	test('should have valid percentage and currency formats', async ({
		freeApi,
	}) => {
		const response = await freeApi.getStocks({ limit: 3 });

		response.expectStatus(200);
		const body = await response.json();

		for (const stock of body.data.data) {
			expect(stock.DividendYield).toContain('%');
			expect(stock.ROCE).toContain('%');
			expect(stock.ROE).toContain('%');
			expect(stock.FaceValue).toContain('₹');
		}
	});
});
