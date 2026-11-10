import { InjectionToken } from '@angular/core';

export class AdminFeedbackError extends Error {}

// Browser decoding honors EXIF orientation; canvas normalizes pixels before metadata is removed.
export async function prepareAdminImage(file: File): Promise<File> {
  const url = URL.createObjectURL(file);
  try {
    const image = new Image();
    await new Promise<void>((resolve, reject) => {
      image.onload = () => resolve();
      image.onerror = () =>
        reject(new AdminFeedbackError('This image could not be read. Choose a valid JPEG or PNG.'));
      image.src = url;
    });
    const scale = Math.min(
      1,
      4096 / image.naturalWidth,
      4096 / image.naturalHeight,
      Math.sqrt(16_000_000 / (image.naturalWidth * image.naturalHeight)),
    );
    const canvas = document.createElement('canvas');
    canvas.width = Math.max(1, Math.floor(image.naturalWidth * scale));
    canvas.height = Math.max(1, Math.floor(image.naturalHeight * scale));
    const context = canvas.getContext('2d');
    if (!context)
      throw new AdminFeedbackError(
        'Image processing is unavailable in this browser. Please try another browser.',
      );
    context.drawImage(image, 0, 0, canvas.width, canvas.height);
    const blob = await new Promise<Blob>((resolve, reject) =>
      canvas.toBlob(
        (value) =>
          value
            ? resolve(value)
            : reject(
                new AdminFeedbackError(
                  'The image could not be processed. Try a smaller JPEG or PNG.',
                ),
              ),
        file.type,
        0.9,
      ),
    );
    if (blob.size > 5 * 1024 * 1024)
      throw new AdminFeedbackError(
        'The processed image exceeds 5 MB. Please choose a smaller image.',
      );
    return new File([blob], file.name, { type: file.type });
  } finally {
    URL.revokeObjectURL(url);
  }
}
export const ADMIN_IMAGE_PREPARER = new InjectionToken<(file: File) => Promise<File>>(
  'ADMIN_IMAGE_PREPARER',
  { providedIn: 'root', factory: () => prepareAdminImage },
);
