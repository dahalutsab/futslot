const KEEP_AS_IS = ['image/jpeg', 'image/png', 'image/webp'];

/**
 * A photo ready to upload: at most {@code maxEdge} pixels on its longest side, as JPEG. Phone photos are often
 * several MB; this keeps uploads quick on mobile data and under the API's 5 MB limit. Small photos in a format
 * the API takes are sent unchanged.
 */
export async function shrink(file: File, maxEdge = 2000, quality = 0.85): Promise<Blob> {
  let bitmap: ImageBitmap;
  try {
    bitmap = await createImageBitmap(file, { imageOrientation: 'from-image' });
  }
  catch {
    throw new Error(`${file.name} isn't a photo this browser can read. Use a JPEG, PNG or WebP.`);
  }
  const scale = Math.min(1, maxEdge / Math.max(bitmap.width, bitmap.height));
  if (scale === 1 && file.size <= 1_500_000 && KEEP_AS_IS.includes(file.type)) {
    bitmap.close();
    return file;
  }
  const canvas = document.createElement('canvas');
  canvas.width = Math.round(bitmap.width * scale);
  canvas.height = Math.round(bitmap.height * scale);
  canvas.getContext('2d')!.drawImage(bitmap, 0, 0, canvas.width, canvas.height);
  bitmap.close();
  return new Promise((resolve, reject) => canvas.toBlob(
    (blob) => (blob ? resolve(blob) : reject(new Error(`Couldn't prepare ${file.name}.`))), 'image/jpeg', quality));
}
