import fs from 'node:fs';
import path from 'node:path';
import test from '@lib/BaseTest';
import { expect } from '@playwright/test';
import type { FileDownloaderPage } from '@/pageFactory/ui/FileDownloaderPage';

const DOWNLOAD_DIR = path.join(process.cwd(), 'temp-downloads');

function fileDownloaderTests(
	pageFixtureName: 'fileDownloaderPage' | 'secureFileDownloaderPage',
) {
	const getPage = (fixtures: {
		fileDownloaderPage: FileDownloaderPage;
		secureFileDownloaderPage: FileDownloaderPage;
	}) =>
		pageFixtureName === 'fileDownloaderPage'
			? fixtures.fileDownloaderPage
			: fixtures.secureFileDownloaderPage;

	test.describe(`File Downloader (${pageFixtureName})`, () => {
		test.beforeEach(
			async ({ fileDownloaderPage, secureFileDownloaderPage }) => {
				const downloaderPage = getPage({
					fileDownloaderPage,
					secureFileDownloaderPage,
				});
				await downloaderPage.goto();
			},
		);

		test.afterEach(() => {
			if (fs.existsSync(DOWNLOAD_DIR)) {
				fs.rmSync(DOWNLOAD_DIR, { recursive: true });
			}
		});

		test.describe('Page Structure', () => {
			test('should display the heading', async ({
				fileDownloaderPage,
				secureFileDownloaderPage,
			}) => {
				const downloaderPage = getPage({
					fileDownloaderPage,
					secureFileDownloaderPage,
				});
				await expect(downloaderPage.heading).toContainText('File Downloader');
			});

			test('should have download links on the page', async ({
				fileDownloaderPage,
				secureFileDownloaderPage,
			}) => {
				const downloaderPage = getPage({
					fileDownloaderPage,
					secureFileDownloaderPage,
				});
				const fileCount = await downloaderPage.getFileCount();
				expect(fileCount).toBeGreaterThan(0);
			});

			test('should list files with valid names', async ({
				fileDownloaderPage,
				secureFileDownloaderPage,
			}) => {
				const downloaderPage = getPage({
					fileDownloaderPage,
					secureFileDownloaderPage,
				});
				const fileNames = await downloaderPage.getAllFileNames();
				for (const fileName of fileNames) {
					expect(fileName.trim()).toBeTruthy();
				}
			});
		});

		test.describe('File Links', () => {
			test('every link should have a href property', async ({
				fileDownloaderPage,
				secureFileDownloaderPage,
			}) => {
				const downloaderPage = getPage({
					fileDownloaderPage,
					secureFileDownloaderPage,
				});
				const count = await downloaderPage.getFileCount();
				for (let i = 0; i < count; i++) {
					const link = downloaderPage.downloadLinks.nth(i);
					const href = await link.getAttribute('href');
					expect(href).toBeTruthy();
				}
			});

			test('href should contain "download" path', async ({
				fileDownloaderPage,
				secureFileDownloaderPage,
			}) => {
				const downloaderPage = getPage({
					fileDownloaderPage,
					secureFileDownloaderPage,
				});
				const href = await downloaderPage.downloadLinks
					.first()
					.getAttribute('href');
				expect(href).toContain('download');
			});

			test('should be able to filter files by extension', async ({
				fileDownloaderPage,
				secureFileDownloaderPage,
			}) => {
				const fileType = '.txt';
				const downloaderPage = getPage({
					fileDownloaderPage,
					secureFileDownloaderPage,
				});
				const totalFileOfType =
					await downloaderPage.getFileNamesByExtension(fileType);
				expect(totalFileOfType.length).toBeGreaterThan(0);

				for (const name of totalFileOfType) {
					expect(name.trim()).toMatch(/\.txt$/);
				}
			});

			test('should check if a known file exists', async ({
				fileDownloaderPage,
				secureFileDownloaderPage,
			}) => {
				const downloaderPage = getPage({
					fileDownloaderPage,
					secureFileDownloaderPage,
				});
				const allFiles = await downloaderPage.getAllFileNames();
				const firstFile = allFiles[0].trim();

				const exists = await downloaderPage.isFileListed(firstFile);
				expect(exists).toBe(true);
			});

			test('should return false for a non-existent file', async ({
				fileDownloaderPage,
				secureFileDownloaderPage,
			}) => {
				const downloaderPage = getPage({
					fileDownloaderPage,
					secureFileDownloaderPage,
				});
				const exists = await downloaderPage.isFileListed(
					'this-file-does-not-exist.xyz',
				);
				expect(exists).toBe(false);
			});
		});

		test.describe('File Download', () => {
			test('should download a file by clicking its link @smoke', async ({
				fileDownloaderPage,
				secureFileDownloaderPage,
			}) => {
				const downloaderPage = getPage({
					fileDownloaderPage,
					secureFileDownloaderPage,
				});

				const fileNames = await downloaderPage.getAllFileNames();
				const targetFile = fileNames[0].trim();

				const download = await downloaderPage.downloadFileByName(targetFile);

				expect(await download.failure()).toBeNull();
				expect(download.suggestedFilename).toBeTruthy();
			});

			test('downloaded file should have a valid suggested filename', async ({
				fileDownloaderPage,
				secureFileDownloaderPage,
			}) => {
				const downloaderPage = getPage({
					fileDownloaderPage,
					secureFileDownloaderPage,
				});
				const download = await downloaderPage.downloadFileByIndex(0);
				const suggestedName = download.suggestedFilename();
				expect(suggestedName).toBeTruthy();
				expect(suggestedName).toMatch(/\.\w+$/);
			});

			test('should download file to disk and verify it exists', async ({
				fileDownloaderPage,
				secureFileDownloaderPage,
			}) => {
				const downloaderPage = getPage({
					fileDownloaderPage,
					secureFileDownloaderPage,
				});

				const targetFile =
					await test.step('Get first available file name', async () => {
						const fileNames = await downloaderPage.getAllFileNames();
						return fileNames[0].trim();
					});

				await test.step('Download and save file to disk', async () => {
					if (!fs.existsSync(DOWNLOAD_DIR)) {
						fs.mkdirSync(DOWNLOAD_DIR, { recursive: true });
					}
					const savePath = path.join(DOWNLOAD_DIR, targetFile);
					await downloaderPage.downloadAndSaveTo(targetFile, savePath);
				});

				await test.step('Verify file exists and has content', () => {
					const savePath = path.join(DOWNLOAD_DIR, targetFile);
					expect(fs.existsSync(savePath)).toBe(true);
					const stats = fs.statSync(savePath);
					expect(stats.size).toBeGreaterThan(0);
				});
			});
		});
	});
}

fileDownloaderTests('fileDownloaderPage');
fileDownloaderTests('secureFileDownloaderPage');
