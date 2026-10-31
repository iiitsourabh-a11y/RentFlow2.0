import { useState, useEffect } from 'react'
import axios from 'axios'
import {
  Search, MapPin, Home, KeyRound, ArrowRight, ShieldCheck, X,
  LoaderCircle, Bed, Bath, PlusCircle, LogOut, User as UserIcon, Trash2
} from 'lucide-react'
import './App.css'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080',
})

export type Property = {
  id: number
  title: string
  description?: string
  rentAmount: number
  propertyType: string
  address: string
  city: string
  bedrooms: number
  bathrooms: number
  available: boolean
  ownerId: number
  ownerName: string
  createdAt?: string
  updatedAt?: string
}

export type CurrentUser = {
  userId: number
  fullName: string
  email: string
  role: 'TENANT' | 'OWNER' | 'ADMIN'
}

function App() {
  const [query, setQuery] = useState('')
  const [minRent, setMinRent] = useState('')
  const [maxRent, setMaxRent] = useState('')
  const [status, setStatus] = useState('')
  const [properties, setProperties] = useState<Property[]>([])
  const [modal, setModal] = useState<'login' | 'register' | 'createProperty' | null>(null)
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState('')
  const [currentUser, setCurrentUser] = useState<CurrentUser | null>(null)

  const [authForm, setAuthForm] = useState({
    fullName: '', email: '', password: '', phone: '', role: 'TENANT',
  })

  const [propertyForm, setPropertyForm] = useState({
    title: '',
    description: '',
    rentAmount: '',
    propertyType: 'Apartment',
    address: '',
    city: '',
    bedrooms: '1',
    bathrooms: '1',
  })

  async function fetchProperties(cityQuery?: string, min?: string, max?: string) {
    setLoading(true)
    setMessage('')
    try {
      const params: Record<string, string> = { page: '0', size: '24' }
      const q = cityQuery !== undefined ? cityQuery : query
      const mn = min !== undefined ? min : minRent
      const mx = max !== undefined ? max : maxRent

      if (q.trim()) params.city = q.trim()
      if (mn.trim()) params.minRent = mn.trim()
      if (mx.trim()) params.maxRent = mx.trim()

      const r = await api.get('/api/v1/properties', { params })
      const data = r.data
      const list: Property[] = Array.isArray(data) ? data : data.content || []
      setProperties(list)
      const total = Array.isArray(data) ? data.length : (data.totalElements ?? list.length)
      setStatus(`Showing ${total} available properties`)
    } catch (e: unknown) {
      const err = e as { response?: { status?: number; data?: { message?: string } } }
      if (err.response?.status === 401) {
        setStatus('Property search requires login under current rules.')
      } else {
        setStatus('Could not load properties. Ensure backend is running.')
      }
    } finally {
      setLoading(false)
    }
  }

  // Restore session on component mount & fetch initial properties
  useEffect(() => {
    const token = localStorage.getItem('rentflow_token')
    if (token) {
      api.defaults.headers.common.Authorization = `Bearer ${token}`
      api.get('/api/v1/auth/me')
        .then(res => {
          setCurrentUser(res.data)
        })
        .catch(() => {
          localStorage.removeItem('rentflow_token')
          delete api.defaults.headers.common.Authorization
        })
    }
    fetchProperties('', '', '')
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  function handleLogout() {
    localStorage.removeItem('rentflow_token')
    delete api.defaults.headers.common.Authorization
    setCurrentUser(null)
    setMessage('Logged out successfully.')
  }

  function changeAuthForm(e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) {
    setAuthForm({ ...authForm, [e.target.name]: e.target.value })
  }

  function changePropertyForm(e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) {
    setPropertyForm({ ...propertyForm, [e.target.name]: e.target.value })
  }

  async function checkBackend() {
    setLoading(true)
    try {
      const r = await api.get('/actuator/health')
      setStatus(`Backend status: ${r.data.status}`)
    } catch {
      setStatus('Backend is unreachable. Start Spring Boot on port 8080.')
    } finally {
      setLoading(false)
    }
  }

  async function submitAuth(e: React.FormEvent) {
    e.preventDefault()
    setLoading(true)
    setMessage('')
    try {
      const endpoint = modal === 'register'
        ? '/api/v1/auth/register'
        : '/api/v1/auth/login'
      const payload = modal === 'register'
        ? {
            fullName: authForm.fullName,
            email: authForm.email,
            password: authForm.password,
            phone: authForm.phone || null,
            role: authForm.role,
          }
        : { email: authForm.email, password: authForm.password }

      const r = await api.post(endpoint, payload)
      const data = r.data
      const token = data.token || data.accessToken || data.access_token

      if (token) {
        localStorage.setItem('rentflow_token', token)
        api.defaults.headers.common.Authorization = `Bearer ${token}`
        const meRes = await api.get('/api/v1/auth/me')
        setCurrentUser(meRes.data)
      }

      setMessage(modal === 'register' ? 'Account created successfully!' : 'Login successful!')
      setModal(null)
      setStatus(`Welcome, ${data.fullName || 'back'}!`)
      fetchProperties()
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string; fieldErrors?: Record<string, string> } } }
      const errData = err.response?.data
      if (errData?.fieldErrors) {
        const firstErr = Object.values(errData.fieldErrors)[0]
        setMessage(firstErr || 'Validation failed')
      } else {
        setMessage(errData?.message || 'Request failed. Please try again.')
      }
    } finally {
      setLoading(false)
    }
  }

  async function submitProperty(e: React.FormEvent) {
    e.preventDefault()
    setLoading(true)
    setMessage('')
    try {
      const payload = {
        title: propertyForm.title.trim(),
        description: propertyForm.description.trim() || undefined,
        rentAmount: parseFloat(propertyForm.rentAmount),
        propertyType: propertyForm.propertyType,
        address: propertyForm.address.trim(),
        city: propertyForm.city.trim(),
        bedrooms: parseInt(propertyForm.bedrooms, 10),
        bathrooms: parseInt(propertyForm.bathrooms, 10),
        available: true,
      }

      await api.post('/api/v1/properties', payload)
      setMessage('Property listed successfully!')
      setModal(null)
      setPropertyForm({
        title: '',
        description: '',
        rentAmount: '',
        propertyType: 'Apartment',
        address: '',
        city: '',
        bedrooms: '1',
        bathrooms: '1',
      })
      fetchProperties()
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string; fieldErrors?: Record<string, string> } } }
      const errData = err.response?.data
      if (errData?.fieldErrors) {
        const firstErr = Object.values(errData.fieldErrors)[0]
        setMessage(firstErr || 'Validation failed.')
      } else {
        setMessage(errData?.message || 'Failed to list property.')
      }
    } finally {
      setLoading(false)
    }
  }

  async function deleteProperty(id: number) {
    if (!confirm('Are you sure you want to remove this property listing?')) return
    try {
      await api.delete(`/api/v1/properties/${id}`)
      setMessage('Property listing deleted.')
      fetchProperties()
    } catch {
      setMessage('Failed to delete property. You may only delete your own listings.')
    }
  }

  return (
    <main>
      <nav className="navbar">
        <a className="brand" href="#">
          <span className="brand-icon"><Home size={21}/></span> rentflow<span className="dot">.</span>
        </a>
        <div className="nav-links">
          <a href="#explore">Explore stays</a>
          <a href="#how">How it works</a>
        </div>
        <div className="nav-actions">
          {currentUser ? (
            <div className="user-nav">
              <span className="user-badge">
                <UserIcon size={14}/> {currentUser.fullName} ({currentUser.role})
              </span>
              {currentUser.role === 'OWNER' && (
                <button
                  className="btn-dark btn-sm"
                  onClick={() => { setMessage(''); setModal('createProperty') }}
                >
                  <PlusCircle size={16}/> List Property
                </button>
              )}
              <button className="btn-outline btn-sm" onClick={handleLogout}>
                <LogOut size={15}/> Logout
              </button>
            </div>
          ) : (
            <>
              <button className="btn-outline" onClick={() => { setMessage(''); setModal('login') }}>
                Log in
              </button>
              <button className="btn-dark" onClick={() => { setMessage(''); setModal('register') }}>
                Get started <ArrowRight size={16}/>
              </button>
            </>
          )}
        </div>
      </nav>

      <section className="hero">
        <div className="hero-copy">
          <span className="eyebrow"><span className="live-dot"/> RENTING, REIMAGINED</span>
          <h1>Find a place<br/>to <span>call home.</span></h1>
          <p>Discover spaces that fit your life. Verified listings, direct owner connections, transparent renting.</p>
          <div className="search-card">
            <label>
              <MapPin size={19}/>
              <span>
                <small>LOCATION / CITY</small>
                <input
                  value={query}
                  onChange={e => setQuery(e.target.value)}
                  onKeyDown={e => e.key === 'Enter' && fetchProperties()}
                  placeholder="Mumbai, Bengaluru, Delhi..."
                />
              </span>
            </label>
            <button onClick={() => fetchProperties()} disabled={loading}>
              <Search size={18}/> Search homes
            </button>
          </div>
          <div className="trust"><ShieldCheck size={17}/> Simple search. Clear choices. Better renting.</div>
        </div>
        <div className="hero-art">
          <img
            src="https://images.unsplash.com/photo-1600210492486-724fe5c67fb0?auto=format&fit=crop&w=1200&q=85"
            alt="Warm, modern living room"
          />
          <div className="floating-card">
            <span className="float-icon"><KeyRound size={20}/></span>
            <span><b>Your next home awaits</b><small>Find a space that feels right.</small></span>
          </div>
          <div className="image-label">A LITTLE MORE ROOM TO LIVE.</div>
        </div>
      </section>

      <section className="features" id="how">
        <div><span>01</span><h3>Find your fit</h3><p>Explore homes and narrow down what matters to you.</p></div>
        <div><span>02</span><h3>Know the details</h3><p>Review property specs, rent amount, and owner contacts transparently.</p></div>
        <div><span>03</span><h3>Move forward</h3><p>Direct communication with property owners with zero hassle.</p></div>
      </section>

      <section className="explore" id="explore">
        <div className="explore-header">
          <div>
            <span className="eyebrow">YOUR NEXT CHAPTER</span>
            <h2>Spaces worth coming home to.</h2>
            <p>{status || 'Search available properties by city.'}</p>
          </div>
          <div className="explore-filters">
            <input
              type="number"
              placeholder="Min Rent ($)"
              value={minRent}
              onChange={e => setMinRent(e.target.value)}
              className="filter-input"
            />
            <input
              type="number"
              placeholder="Max Rent ($)"
              value={maxRent}
              onChange={e => setMaxRent(e.target.value)}
              className="filter-input"
            />
            <button className="btn-outline btn-sm" onClick={() => fetchProperties()} disabled={loading}>
              Filter
            </button>
            <button className="btn-dark btn-sm" onClick={checkBackend} disabled={loading}>
              {loading ? <LoaderCircle size={15}/> : null} Health Check
            </button>
          </div>
        </div>

        {properties.length > 0 ? (
          <div className="property-grid">
            {properties.map(p => (
              <article key={p.id} className="property-card">
                <div className="property-badge">{p.propertyType}</div>
                <h3>{p.title}</h3>
                <p className="property-location"><MapPin size={14}/> {p.address}, {p.city}</p>
                {p.description && <p className="property-desc">{p.description}</p>}
                <div className="property-specs">
                  <span><Bed size={15}/> {p.bedrooms} Beds</span>
                  <span><Bath size={15}/> {p.bathrooms} Baths</span>
                </div>
                <div className="property-footer">
                  <b className="property-price">
                    ${Number(p.rentAmount).toLocaleString('en-US', { minimumFractionDigits: 0, maximumFractionDigits: 2 })}
                    <small> / month</small>
                  </b>
                  {currentUser && currentUser.userId === p.ownerId ? (
                    <button
                      className="btn-danger-icon"
                      onClick={() => deleteProperty(p.id)}
                      title="Delete your property listing"
                    >
                      <Trash2 size={16}/>
                    </button>
                  ) : (
                    <span className="owner-tag">by {p.ownerName}</span>
                  )}
                </div>
              </article>
            ))}
          </div>
        ) : (
          <div className="empty-state">
            <Home size={40}/>
            <h3>No properties found</h3>
            <p>Try searching for a different city or clearing rent filters.</p>
          </div>
        )}
      </section>

      <section className="auth" id="auth">
        <div>
          <span className="eyebrow">LET'S GET YOU MOVING</span>
          <h2>Your next home is a few steps away.</h2>
          <p>Sign in or create an account to start browsing and listing properties.</p>
        </div>
        <div className="nav-actions">
          {!currentUser ? (
            <>
              <button className="btn-outline" onClick={() => setModal('login')}>Log in</button>
              <button className="btn-dark" onClick={() => setModal('register')}>
                Get started <ArrowRight size={16}/>
              </button>
            </>
          ) : (
            <p className="welcome-text">Logged in as <b>{currentUser.email}</b></p>
          )}
        </div>
      </section>

      <footer>
        <a className="brand" href="#">rentflow<span className="dot">.</span></a>
        <span>Find your place. Start your next chapter.</span>
        <span>© 2026 RentFlow — Production Platform</span>
      </footer>

      {/* Auth Modal (Login / Register) */}
      {modal && modal !== 'createProperty' && (
        <div className="modal-backdrop" onMouseDown={e => { if (e.target === e.currentTarget) setModal(null) }}>
          <section className="auth-modal" role="dialog" aria-modal="true" aria-labelledby="auth-title">
            <button className="modal-close" onClick={() => setModal(null)} aria-label="Close"><X size={20}/></button>
            <span className="eyebrow">WELCOME TO RENTFLOW</span>
            <h2 id="auth-title">{modal === 'register' ? 'Create your account' : 'Welcome back'}</h2>
            <p>{modal === 'register' ? 'Start finding your next home.' : 'Log in to continue to RentFlow.'}</p>
            <form onSubmit={submitAuth}>
              {modal === 'register' && (
                <>
                  <label>Full name
                    <input
                      name="fullName"
                      value={authForm.fullName}
                      onChange={changeAuthForm}
                      required
                      maxLength={100}
                      placeholder="Your full name"
                    />
                  </label>
                  <label>Phone (optional)
                    <input
                      name="phone"
                      value={authForm.phone}
                      onChange={changeAuthForm}
                      maxLength={20}
                      placeholder="e.g. +1 555-0199"
                    />
                  </label>
                  <label>Account type
                    <select name="role" value={authForm.role} onChange={changeAuthForm}>
                      <option value="TENANT">Tenant (Search & Rent)</option>
                      <option value="OWNER">Property Owner (List & Manage)</option>
                    </select>
                  </label>
                </>
              )}
              <label>Email address
                <input
                  name="email"
                  type="email"
                  value={authForm.email}
                  onChange={changeAuthForm}
                  required
                  placeholder="you@example.com"
                />
              </label>
              <label>Password
                <input
                  name="password"
                  type="password"
                  value={authForm.password}
                  onChange={changeAuthForm}
                  required
                  minLength={modal === 'register' ? 8 : 1}
                  placeholder={modal === 'register' ? 'At least 8 characters' : 'Your password'}
                />
              </label>
              {message && <p className="form-message" role="alert">{message}</p>}
              <button className="btn-dark submit-btn" type="submit" disabled={loading}>
                {loading ? 'Please wait...' : modal === 'register' ? 'Create account' : 'Log in'} <ArrowRight size={17}/>
              </button>
            </form>
            <p className="modal-switch">
              {modal === 'register' ? 'Already have an account?' : 'New to RentFlow?'}{' '}
              <button onClick={() => { setMessage(''); setModal(modal === 'register' ? 'login' : 'register') }}>
                {modal === 'register' ? 'Log in' : 'Create account'}
              </button>
            </p>
          </section>
        </div>
      )}

      {/* Owner Create Property Modal */}
      {modal === 'createProperty' && (
        <div className="modal-backdrop" onMouseDown={e => { if (e.target === e.currentTarget) setModal(null) }}>
          <section className="auth-modal modal-wide" role="dialog" aria-modal="true" aria-labelledby="prop-title">
            <button className="modal-close" onClick={() => setModal(null)} aria-label="Close"><X size={20}/></button>
            <span className="eyebrow">OWNER WORKFLOW</span>
            <h2 id="prop-title">List a New Property</h2>
            <p>Publish a listing for prospective tenants across the platform.</p>
            <form onSubmit={submitProperty}>
              <label>Title
                <input
                  name="title"
                  value={propertyForm.title}
                  onChange={changePropertyForm}
                  required
                  maxLength={150}
                  placeholder="e.g. Modern Sunset Apartment"
                />
              </label>
              <div className="form-row">
                <label>Property Type
                  <select name="propertyType" value={propertyForm.propertyType} onChange={changePropertyForm}>
                    <option value="Apartment">Apartment</option>
                    <option value="House">House</option>
                    <option value="Studio">Studio</option>
                    <option value="Villa">Villa</option>
                    <option value="Condo">Condo</option>
                  </select>
                </label>
                <label>Monthly Rent ($)
                  <input
                    name="rentAmount"
                    type="number"
                    step="0.01"
                    min="0.01"
                    value={propertyForm.rentAmount}
                    onChange={changePropertyForm}
                    required
                    placeholder="1500.00"
                  />
                </label>
              </div>
              <div className="form-row">
                <label>City
                  <input
                    name="city"
                    value={propertyForm.city}
                    onChange={changePropertyForm}
                    required
                    maxLength={100}
                    placeholder="e.g. Mumbai"
                  />
                </label>
                <label>Address
                  <input
                    name="address"
                    value={propertyForm.address}
                    onChange={changePropertyForm}
                    required
                    maxLength={255}
                    placeholder="e.g. 42 Marine Drive"
                  />
                </label>
              </div>
              <div className="form-row">
                <label>Bedrooms
                  <input
                    name="bedrooms"
                    type="number"
                    min="0"
                    max="100"
                    value={propertyForm.bedrooms}
                    onChange={changePropertyForm}
                    required
                  />
                </label>
                <label>Bathrooms
                  <input
                    name="bathrooms"
                    type="number"
                    min="0"
                    max="100"
                    value={propertyForm.bathrooms}
                    onChange={changePropertyForm}
                    required
                  />
                </label>
              </div>
              <label>Description (optional)
                <textarea
                  name="description"
                  value={propertyForm.description}
                  onChange={changePropertyForm}
                  rows={3}
                  placeholder="Details about amenities, furnishing, sunlight, etc."
                />
              </label>
              {message && <p className="form-message" role="alert">{message}</p>}
              <button className="btn-dark submit-btn" type="submit" disabled={loading}>
                {loading ? 'Submitting...' : 'Publish Listing'} <ArrowRight size={17}/>
              </button>
            </form>
          </section>
        </div>
      )}

      {/* Floating toast */}
      {message && !modal && (
        <div className="toast" role="status">
          {message}
          <button onClick={() => setMessage('')}><X size={16}/></button>
        </div>
      )}
    </main>
  )
}

export default App
