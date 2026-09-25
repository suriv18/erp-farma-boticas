export type SupportCatalogItem = {
  codigo: string;
  estado: string;
};

export type FieldDef = {
  name: string;
  label: string;
  type: 'text' | 'textarea' | 'number' | 'checkbox';
};
