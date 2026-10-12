import { NavLink, Outlet, useLocation, useNavigate } from 'react-router';
import { useAuth } from '../../auth/useAuth';
import styles from './Layout.module.css';

export function Layout() {
  const { user, signOut } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const handleSignOut = () => {
    signOut();
    navigate('/');
  };

  return (
    <>
      <header className={styles.header}>
        <div className={`container ${styles.inner}`}>
          <NavLink to="/" className={styles.brand}>
            <img src="/pokeball.svg" alt="" width={28} height={28} />
            PokeBallast
          </NavLink>
          <nav className={styles.nav} aria-label="Main">
            <NavLink to="/" end>
              Browse
            </NavLink>
            <NavLink to="/collection">Collection</NavLink>
          </nav>
          <div className={styles.session}>
            {user ? (
              <>
                <span className={styles.user} title={`Role: ${user.role}`}>
                  {user.username}
                  {user.role === 'ADMIN' && <span className="badge badge-admin">admin</span>}
                </span>
                <button type="button" className="button-link" onClick={handleSignOut}>
                  Sign out
                </button>
              </>
            ) : (
              // come back to the current page after signing in
              <NavLink to="/login" state={{ from: location.pathname + location.search }}>
                Sign in
              </NavLink>
            )}
          </div>
        </div>
      </header>
      <main className="container main">
        <Outlet />
      </main>
      <footer className={styles.footer}>
        <div className="container">Data from PokeAPI · PokeBallast</div>
      </footer>
    </>
  );
}
