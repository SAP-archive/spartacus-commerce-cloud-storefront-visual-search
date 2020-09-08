/**
 * List of items detected on uploaded image.
 */
export interface VisualSearchImageData {
  boundingBoxes: BoundingBoxes[];
}

/**
 * Item detected on uploaded image.
 * id - generated id
 * label - category of the item
 * x1, x2, y1, y2 - coordinates of a bounding box, as percentages
 */
export interface BoundingBoxes {
  id: string;
  label: string;
  x1: number;
  y1: number;
  x2: number;
  y2: number;
}
