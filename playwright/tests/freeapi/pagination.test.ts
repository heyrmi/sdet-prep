import test from '@lib/FreeApiBaseTest';
import { expect } from '@playwright/test';

test.describe('Pagination', () => {
	test('first page should have previousPage false and nextPage true', async ({
		freeApi,
	}) => {
		const response = await freeApi.getUsers({ page: 1, limit: 5 });

		response.expectStatus(200);
		const body = await response.json();

		expect(body.data.page).toBe(1);
		expect(body.data.previousPage).toBe(false);
		expect(body.data.nextPage).toBe(true);
	});

	test('last page should have nextPage false and previousPage true', async ({
		freeApi,
	}) => {
		// First get total pages
		const initial = await freeApi.getUsers({ page: 1, limit: 10 });
		const initialBody = await initial.json();
		const lastPage = initialBody.data.totalPages;

		const response = await freeApi.getUsers({ page: lastPage, limit: 10 });
		response.expectStatus(200);
		const body = await response.json();

		expect(body.data.page).toBe(lastPage);
		expect(body.data.nextPage).toBe(false);
		expect(body.data.previousPage).toBe(true);
	});

	test('should respect different limit values', async ({ freeApi }) => {
		const limits = [1, 10, 50];

		for (const limit of limits) {
			const response = await freeApi.getUsers({ page: 1, limit });
			response.expectStatus(200);
			const body = await response.json();

			expect(body.data.limit).toBe(limit);
			expect(body.data.currentPageItems).toBeLessThanOrEqual(limit);
		}
	});

	test('totalPages should be consistent with totalItems and limit', async ({
		freeApi,
	}) => {
		const response = await freeApi.getProducts({ page: 1, limit: 7 });

		response.expectStatus(200);
		const body = await response.json();

		const expectedPages = Math.ceil(body.data.totalItems / body.data.limit);
		expect(body.data.totalPages).toBe(expectedPages);
	});

	test('pagination should work consistently across resource types', async ({
		freeApi,
	}) => {
		const endpoints = [
			() => freeApi.getUsers({ page: 1, limit: 2 }),
			() => freeApi.getProducts({ page: 1, limit: 2 }),
			() => freeApi.getJokes({ page: 1, limit: 2 }),
			() => freeApi.getBooks({ page: 1, limit: 2 }),
			() => freeApi.getStocks({ page: 1, limit: 2 }),
		];

		for (const endpoint of endpoints) {
			const response = await endpoint();
			response.expectStatus(200);
			const body = await response.json();

			// All paginated responses should have the same envelope keys
			expect(body.data).toHaveProperty('page');
			expect(body.data).toHaveProperty('limit');
			expect(body.data).toHaveProperty('totalPages');
			expect(body.data).toHaveProperty('previousPage');
			expect(body.data).toHaveProperty('nextPage');
			expect(body.data).toHaveProperty('totalItems');
			expect(body.data).toHaveProperty('currentPageItems');
			expect(body.data).toHaveProperty('data');
		}
	});
});
