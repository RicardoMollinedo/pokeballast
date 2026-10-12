import { lazy, Suspense } from 'react';
import { Link, Route, Routes } from 'react-router';
import { RequireAuth } from '../auth/RequireAuth';
import { CatalogPage } from '../features/catalog/pages/CatalogPage';
import { PokemonDetailPage } from '../features/catalog/pages/PokemonDetailPage';
import { CollectionPage } from '../features/collection/pages/CollectionPage';
import { Layout } from '../shared/components/Layout';
import { EmptyState, Loading } from '../shared/components/Status';

// secondary routes are loaded on demand to keep the first bundle small
const EditPokemonPage = lazy(() =>
  import('../features/collection/pages/EditPokemonPage').then((module) => ({ default: module.EditPokemonPage })));
const LoginPage = lazy(() => import('../features/auth/pages/LoginPage').then((module) => ({ default: module.LoginPage })));
const RegisterPage = lazy(() =>
  import('../features/auth/pages/RegisterPage').then((module) => ({ default: module.RegisterPage })));

export function App() {
  return (
    <Suspense fallback={<Loading />}>
      <Routes>
        <Route element={<Layout />}>
          <Route index element={<CatalogPage />} />
          <Route path="pokemon/:idOrName" element={<PokemonDetailPage />} />
          <Route path="collection" element={<CollectionPage />} />
          <Route path="collection/:id/edit"
            element={<RequireAuth roles={['TRAINER', 'ADMIN']}><EditPokemonPage /></RequireAuth>} />
          <Route path="login" element={<LoginPage />} />
          <Route path="register" element={<RegisterPage />} />
          <Route path="*" element={<EmptyState>Page not found. <Link to="/">Go to the list</Link></EmptyState>} />
        </Route>
      </Routes>
    </Suspense>
  );
}
