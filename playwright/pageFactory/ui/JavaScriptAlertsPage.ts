import type { Locator, Page } from '@playwright/test';
import { BasePage } from './BasePage';

export class JavaScriptAlertPage extends BasePage {
	readonly heading: Locator;
	readonly jsAlertButton: Locator;
	readonly jsConfirmButton: Locator;
	readonly jsPromptButton: Locator;
	readonly result: Locator;

	constructor(page: Page) {
		super(page);

		this.heading = page.locator('h3');

		this.jsAlertButton = page.getByRole('button', {
			name: 'Click for JS Alert',
		});
		this.jsConfirmButton = page.getByRole('button', {
			name: 'Click for JS Confirm',
		});
		this.jsPromptButton = page.getByRole('button', {
			name: 'Click for JS Prompt',
		});
		this.result = page.locator('#result');
	}

	async goto(): Promise<void> {
		await this.page.goto('/javascript_alerts');
	}

	async triggerAlertAndAccept(): Promise<string> {
		let alertMessage = '';

		this.page.once('dialog', async (dialog) => {
			alertMessage = dialog.message();
			await dialog.accept();
		});

		await this.jsAlertButton.click();
		return alertMessage;
	}

	async triggerConfirmAndAccept(): Promise<string> {
		let confirmMessage = '';

		this.page.once('dialog', async (dialog) => {
			confirmMessage = dialog.message();
			await dialog.accept();
		});
		await this.jsConfirmButton.click();

		return confirmMessage;
	}

	async triggerConfirmAndDismiss(): Promise<string> {
		let confirmMessage = '';

		this.page.once('dialog', async (dialog) => {
			confirmMessage = dialog.message();
			await dialog.dismiss();
		});
		await this.jsConfirmButton.click();

		return confirmMessage;
	}

	async triggerPromptAndEnterText(text: string): Promise<string> {
		let promptMessage = '';

		this.page.once('dialog', async (dialog) => {
			promptMessage = dialog.message();
			await dialog.accept(text);
		});

		await this.jsPromptButton.click();
		return promptMessage;
	}

	async triggerPromptAndDismiss(): Promise<string> {
		let promptMessage = '';

		this.page.once('dialog', async (dialog) => {
			promptMessage = dialog.message();
			await dialog.dismiss();
		});

		await this.jsPromptButton.click();

		return promptMessage;
	}
}
