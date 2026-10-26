import { useState } from 'react'
import axios from 'axios'
import { Search, MapPin, Home, KeyRound, ArrowRight, ShieldCheck, X, LoaderCircle } from 'lucide-react'
import './App.css'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080',
})

type Property = {
  id?: number
  title?: string
  name?: string
  city?: string
  location?: string
  rent?: number
  monthlyRent?: number
  description?: string
}

function App() {
  const [query, setQuery] = useState('')
  const [status, setStatus] = useState('')
  const [properties, setProperties] = useState<Property[]>([])
  const [modal, setModal] = useState<'login' | 'register' | null>(null)
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState('')
  const [form, setForm] = useState({
    fullName: '', email: '', password: '', phone: '', role: 'TENANT',
  })

  function changeForm(e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) {
    setForm({ ...form, [e.target.name]: e.target.value })
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

  async function loadProperties() {
    setLoading(true)
    setMessage('')
    try {
      const params: Record<string, string> = { page: '0', size: '12' }
      if (query.trim()) params.city = query.trim()
      const r = await api.get('/api/v1/properties', { params })
      const data = r.data
      setProperties(Array.isArray(data) ? data : data.content || [])
      setStatus(`Found ${Array.isArray(data) ? data.length : (data.totalElements ?? data.content?.length ?? 0)} properties`)
      document.getElementById('explore')?.scrollIntoView({ behavior: 'smooth' })
    } catch (e: any) {
      setStatus(e.response?.status === 401
        ? 'Property search requires login under the current backend security rules.'
        : 'Could not load properties. Check that the backend is running.')
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
        ? { fullName: form.fullName, email: form.email, password: form.password, phone: form.phone || null, role: form.role }
        : { email: form.email, password: form.password }
      const r = await api.post(endpoint, payload)
      const data = r.data
      const token = data.token || data.accessToken || data.access_token
      if (token) {
        localStorage.setItem('rentflow_token', token)
        api.defaults.headers.common.Authorization = `Bearer ${token}`
      }
      setMessage(modal === 'register' ? 'Account created successfully.' : 'Login successful.')
      setModal(null)
      setStatus(`Welcome${data.fullName ? `, ${data.fullName}` : ''}!`)
    } catch (e: any) {
      setMessage(e.response?.data?.message || e.response?.data?.detail || 'Request failed. Check your details and try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <main>
      <nav className="navbar">
        <a className="brand" href="#"><span className="brand-icon"><Home size={21}/></span> rentflow<span className="dot">.</span></a>
        <div className="nav-links"><a href="#explore">Explore stays</a><a href="#how">How it works</a></div>
        <div className="nav-actions">
          <button className="btn-outline" onClick={() => { setMessage(''); setModal('login') }}>Log in</button>
          <button className="btn-dark" onClick={() => { setMessage(''); setModal('register') }}>Get started <ArrowRight size={16}/></button>
        </div>
      </nav>

      <section className="hero">
        <div className="hero-copy">
          <span className="eyebrow"><span className="live-dot"/> RENTING, REIMAGINED</span>
          <h1>Find a place<br/>to <span>call home.</span></h1>
          <p>Discover spaces that fit your life. Your next chapter starts with the right place.</p>
          <div className="search-card">
            <label><MapPin size={19}/><span><small>LOCATION / CITY</small><input value={query} onChange={e => setQuery(e.target.value)} placeholder="City, neighbourhood..."/></span></label>
            <button onClick={loadProperties} disabled={loading}><Search size={18}/> Search homes</button>
          </div>
          <div className="trust"><ShieldCheck size={17}/> Simple search. Clear choices. Better renting.</div>
        </div>
        <div className="hero-art">
          <img src="https://images.unsplash.com/photo-1600210492486-724fe5c67fb0?auto=format&fit=crop&w=1200&q=85" alt="Warm, modern living room"/>
          <div className="floating-card"><span className="float-icon"><KeyRound size={20}/></span><span><b>Your next home awaits</b><small>Find a space that feels right.</small></span></div>
          <div className="image-label">A LITTLE MORE ROOM TO LIVE.</div>
        </div>
      </section>

      <section className="features" id="how">
        <div><span>01</span><h3>Find your fit</h3><p>Explore homes and narrow down what matters to you.</p></div>
        <div><span>02</span><h3>Know the details</h3><p>Review property information before making a decision.</p></div>
        <div><span>03</span><h3>Move forward</h3><p>Get closer to a place you can call your own.</p></div>
      </section>

      <section className="explore" id="explore">
        <div><span className="eyebrow">YOUR NEXT CHAPTER</span><h2>Spaces worth coming home to.</h2><p>{status || 'Search available properties by city.'}</p></div>
        <button className="btn-dark" onClick={checkBackend} disabled={loading}>{loading ? <LoaderCircle size={17}/> : null} Check connection <ArrowRight size={16}/></button>
        {properties.length > 0 && <div className="property-grid">{properties.map((p, i) => <article key={p.id ?? i}><h3>{p.title ?? p.name ?? `Property ${i + 1}`}</h3><p>{p.city ?? p.location ?? 'Location not provided'}</p><p>{p.description ?? ''}</p><b>{p.rent || p.monthlyRent ? `?${p.rent ?? p.monthlyRent} / month` : ''}</b></article>)}</div>}
      </section>

      <section className="auth" id="auth">
        <div><span className="eyebrow">LET'S GET YOU MOVING</span><h2>Your next home is a few steps away.</h2><p>Sign in or create an account to get started.</p></div>
        <div className="nav-actions"><button className="btn-outline" onClick={() => setModal('login')}>Log in</button><button className="btn-dark" onClick={() => setModal('register')}>Get started <ArrowRight size={16}/></button></div>
      </section>
      <footer><a className="brand" href="#">rentflow<span className="dot">.</span></a><span>Find your place. Start your next chapter.</span><span>© 2026 RentFlow</span></footer>

      {modal && <div className="modal-backdrop" onMouseDown={e => { if (e.target === e.currentTarget) setModal(null) }}>
        <section className="auth-modal" role="dialog" aria-modal="true" aria-labelledby="auth-title">
          <button className="modal-close" onClick={() => setModal(null)} aria-label="Close"><X size={20}/></button>
          <span className="eyebrow">WELCOME TO RENTFLOW</span>
          <h2 id="auth-title">{modal === 'register' ? 'Create your account' : 'Welcome back'}</h2>
          <p>{modal === 'register' ? 'Start finding your next home.' : 'Log in to continue to RentFlow.'}</p>
          <form onSubmit={submitAuth}>
            {modal === 'register' && <>
              <label>Full name<input name="fullName" value={form.fullName} onChange={changeForm} required maxLength={100} placeholder="Your full name"/></label>
              <label>Phone (optional)<input name="phone" value={form.phone} onChange={changeForm} maxLength={20} placeholder="Phone number"/></label>
              <label>Account type<select name="role" value={form.role} onChange={changeForm}><option value="TENANT">Tenant</option><option value="OWNER">Property owner</option></select></label>
            </>}
            <label>Email address<input name="email" type="email" value={form.email} onChange={changeForm} required placeholder="you@example.com"/></label>
            <label>Password<input name="password" type="password" value={form.password} onChange={changeForm} required minLength={modal === 'register' ? 8 : 1} placeholder={modal === 'register' ? 'At least 8 characters' : 'Your password'}/></label>
            {message && <p className="form-message" role="alert">{message}</p>}
            <button className="btn-dark submit-btn" type="submit" disabled={loading}>{loading ? 'Please wait...' : modal === 'register' ? 'Create account' : 'Log in'} <ArrowRight size={17}/></button>
          </form>
          <p className="modal-switch">{modal === 'register' ? 'Already have an account?' : 'New to RentFlow?'} <button onClick={() => { setMessage(''); setModal(modal === 'register' ? 'login' : 'register') }}>{modal === 'register' ? 'Log in' : 'Create account'}</button></p>
        </section>
      </div>}
      {message && !modal && <div className="toast" role="status">{message}<button onClick={() => setMessage('')}><X size={16}/></button></div>}
    </main>
  )
}
export default App
