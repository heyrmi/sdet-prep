import test from '@lib/BaseTest';
import { expect } from '@playwright/test';

test.describe('JavaScript Alert Page', () => {
	test.beforeEach(async ({ javaScriptAlertPage }) => {
		await javaScriptAlertPage.goto();
	});

	test('should display correct heading', async ({ javaScriptAlertPage }) => {
		await expect(javaScriptAlertPage.heading).toHaveText('JavaScript Alerts');
	});

	test.describe('JS Alert', () => {
		test('should handle alert and verify message @smoke', async ({
			javaScriptAlertPage,
		}) => {
			const alertMessage = await javaScriptAlertPage.triggerAlertAndAccept();

			expect(alertMessage).toBe('I am a JS Alert');

			await expect(javaScriptAlertPage.result).toHaveText(
				'You successfully clicked an alert',
			);
		});
	});

	test.describe('JS Confirm', () => {
		test('should handle confirm - accept (OK)', async ({
			javaScriptAlertPage,
		}) => {
			const confirmMessage =
				await javaScriptAlertPage.triggerConfirmAndAccept();

			expect(confirmMessage).toBe('I am a JS Confirm');
			await expect(javaScriptAlertPage.result).toHaveText('You clicked: Ok');
		});

		test('should handle confirm - dismiss (Cancel)', async ({
			javaScriptAlertPage,
		}) => {
			const confirmMessage =
				await javaScriptAlertPage.triggerConfirmAndDismiss();
			expect(confirmMessage).toBe('I am a JS Confirm');
			await expect(javaScriptAlertPage.result).toHaveText(
				'You clicked: Cancel',
			);
		});
	});

	test.describe('JS Prompt', () => {
		test('should handle prompt - enter text and accept', async ({
			javaScriptAlertPage,
		}) => {
			const promptMessage =
				await javaScriptAlertPage.triggerPromptAndEnterText('Hello Rahul!');
			expect(promptMessage).toBe('I am a JS prompt');
			await expect(javaScriptAlertPage.result).toHaveText(
				'You entered: Hello Rahul!',
			);
		});

		test('should handle prompt - dismiss without entering text', async ({
			javaScriptAlertPage,
		}) => {
			const promptMessage = await javaScriptAlertPage.triggerPromptAndDismiss();
			expect(promptMessage).toBe('I am a JS prompt');

			await expect(javaScriptAlertPage.result).toHaveText('You entered: null');
		});

		test('should handle prompt - accept with empty text', async ({
			javaScriptAlertPage,
		}) => {
			const promptMessage =
				await javaScriptAlertPage.triggerPromptAndEnterText('');
			expect(promptMessage).toBe('I am a JS prompt');

			await expect(javaScriptAlertPage.result).toHaveText('You entered:');
		});
	});
});
