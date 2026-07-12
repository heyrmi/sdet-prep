import test from '@lib/BaseTest';
import { expect } from '@playwright/test';
import loginUsers from '@testdata/ui/login-user.json' with { type: 'json' };
import type { LoginTestData } from '@/types/testData';

test.describe('Login Page test', () => {
	test.beforeEach(async ({ loginPage }) => {
		await loginPage.goto();
	});

	test('should display login page correctly', async ({ loginPage }) => {
		await expect(loginPage.heading).toHaveText('Login Page');
		await expect(loginPage.usernameInput).toBeVisible();
		await expect(loginPage.passwordInput).toBeVisible();
		await expect(loginPage.loginButton).toBeVisible();
	});

	for (const user of loginUsers as LoginTestData[]) {
		test(`${user.testName} @smoke`, async ({ loginPage }) => {
			await loginPage.login(user.username, user.password);
			await expect(loginPage.flashMessage).toContainText(user.expectedMessage);

			if (user.shouldSucceed) {
				await expect(loginPage.page).toHaveURL(/\/secure$/);
			}
		});
	}
});
