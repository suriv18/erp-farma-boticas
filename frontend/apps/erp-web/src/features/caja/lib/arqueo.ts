export const diferenciaArqueo = (totalSistema: number, totalDeclarado: number): number =>
  Math.round((totalDeclarado - totalSistema) * 100) / 100;
