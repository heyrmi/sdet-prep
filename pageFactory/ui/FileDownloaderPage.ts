import type { Download, Locator, Page } from '@playwright/test';
import { BasePage } from './BasePage';

export class FileDownloaderPage extends BasePage {
	private readonly route: string;

	readonly heading: Locator;
	readonly downloadLinks: Locator;

	constructor(page: Page, route: string = '/download') {
		super(page);
		this.route = route;

		this.heading = page.locator('h3');
		this.downloadLinks = page.locator('.example a');
	}

	async goto(): Promise<void> {
		await this.page.goto(this.route);
	}

	async getFileCount(): Promise<number> {
		return await this.downloadLinks.count();
	}

	async getAllFileNames(): Promise<string[]> {
		return await this.downloadLinks.allTextContents();
	}

	async isFileListed(fileName: string): Promise<boolean> {
		const link = this.downloadLinks.filter({ hasText: fileName });
		return (await link.count()) > 0;
	}

	async getFileNamesByExtension(extension: string): Promise<string[]> {
		const allFiles = await this.getAllFileNames();
		return allFiles.filter((name) => name.trim().endsWith(extension));
	}

	getFileLinkByName(fileName: string): Locator {
		return this.downloadLinks.filter({ hasText: fileName }).first();
	}

	async downloadFileByName(fileName: string): Promise<Download> {
		const link = this.getFileLinkByName(fileName);

		const [download] = await Promise.all([
			this.page.waitForEvent('download'),
			link.click(),
		]);

		return download;
	}

	async downloadFileByIndex(index: number): Promise<Download> {
		const link = this.downloadLinks.nth(index);

		const [download] = await Promise.all([
			this.page.waitForEvent('download'),
			link.click(),
		]);

		return download;
	}

	async downloadAndSaveTo(
		fileName: string,
		savePath: string,
	): Promise<Download> {
		const download = await this.downloadFileByName(fileName);
		await download.saveAs(savePath);
		return download;
	}

	async getFileHref(fileName: string): Promise<string | null> {
		return await this.getFileLinkByName(fileName).getAttribute('href');
	}
}
