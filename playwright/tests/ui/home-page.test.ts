import test from '@lib/BaseTest';
import { expect } from '@playwright/test';

test.describe('Heroku Home Page', () => {
	test.beforeEach(async ({ homePage }) => {
		await homePage.goto();
	});

	test('should display home page correctly @smoke', async ({ homePage }) => {
		await expect(homePage.page).toHaveTitle('The Internet');
		await expect(homePage.heading).toBeVisible();
		await expect(homePage.heading).toHaveText('Welcome to the-internet');
	});

	test('should display all the links', async ({ homePage }) => {
		expect(await homePage.getLinkCount()).toBe(44);
	});
});
