import test from '@lib/BaseTest';
import { expect } from '@playwright/test';

test.describe('Dynamic Loading Page Tests', () => {
	test.describe('Example 1: Hidden Element', () => {
		test.beforeEach(async ({ dynamicLoadingPage }) => {
			await dynamicLoadingPage.gotoExample1();
		});

		test('should display the heading', async ({ dynamicLoadingPage }) => {
			await expect(dynamicLoadingPage.heading).toHaveText(
				'Dynamically Loaded Page Elements',
			);
			await expect(dynamicLoadingPage.subHeading).toHaveText(
				'Example 1: Element on page that is hidden',
			);
		});

		test('should show "Hello World!" after loading completes @smoke', async ({
			dynamicLoadingPage,
		}) => {
			await expect(dynamicLoadingPage.finishText).toBeHidden();

			await dynamicLoadingPage.clickStart();
			await expect(dynamicLoadingPage.loading).toBeVisible();
			await expect(dynamicLoadingPage.finishText).toBeVisible();
			await expect(dynamicLoadingPage.finishText).toHaveText('Hello World!');
		});

		test('should hide the loading indicator after loading completes', async ({
			dynamicLoadingPage,
		}) => {
			await dynamicLoadingPage.clickStart();
			await expect(dynamicLoadingPage.finishText).toBeVisible();
			await expect(dynamicLoadingPage.loading).toBeHidden();
		});
	});

	test.describe('Example 2: Element Rendered After', () => {
		test.beforeEach(async ({ dynamicLoadingPage }) => {
			await dynamicLoadingPage.gotoExample2();
		});

		test('should display the heading', async ({ dynamicLoadingPage }) => {
			await expect(dynamicLoadingPage.heading).toHaveText(
				'Dynamically Loaded Page Elements',
			);
			await expect(dynamicLoadingPage.subHeading).toHaveText(
				'Example 2: Element rendered after the fact',
			);
		});

		test('should show "Hello World!" after loading completes', async ({
			dynamicLoadingPage,
		}) => {
			await expect(dynamicLoadingPage.finishText).not.toBeAttached();

			await dynamicLoadingPage.clickStart();

			await expect(dynamicLoadingPage.finishText).toBeVisible();
			await expect(dynamicLoadingPage.finishText).toHaveText('Hello World!');
		});

		test('should hide start button after clicking it', async ({
			dynamicLoadingPage,
		}) => {
			await dynamicLoadingPage.clickStart();

			await expect(dynamicLoadingPage.startButton).toBeHidden();
		});
	});
});
