import test from '@lib/BaseTest';
import { expect } from '@playwright/test';

test.describe('Broken Images Page Test', () => {
	test.beforeEach(async ({ brokenImagePage }) => {
		await brokenImagePage.goto();
	});

	test('validate broken page is opened', async ({ brokenImagePage }) => {
		await expect(brokenImagePage.heading).toHaveText('Broken Images');
	});

	test('check all broken images', async ({ brokenImagePage }) => {
		const brokenImageCount = await brokenImagePage.getImageCount('broken');
		const validImageCount = await brokenImagePage.getImageCount('valid');
		expect(brokenImageCount).toBeGreaterThan(0);
		expect(validImageCount).toBeGreaterThan(0);
	});
});
